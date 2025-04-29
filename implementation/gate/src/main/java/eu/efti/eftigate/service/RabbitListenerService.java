package eu.efti.eftigate.service;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.generator.id.MessageIdGenerator;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.service.PlatformIntegrationService.PlatformInfo;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.service.request.UilRequestService;
import eu.efti.eftilogger.model.ComponentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class RabbitListenerService {

    private final GateProperties gateProperties;
    private final SerializeUtils serializeUtils;
    private final RequestSendingService requestSendingService;
    private final RequestServiceFactory requestServiceFactory;
    private final ApIncomingService apIncomingService;
    private final MapperUtils mapperUtils;
    private final LogManager logManager;
    private final MessageIdGenerator messageIdGenerator;
    private final PlatformIntegrationService platformIntegrationService;
    private final UilRequestService uilRequestService;

    @RabbitListener(queues = "${spring.rabbitmq.queues.eftiReceiveMessageQueue:efti.receive-messages.q}")
    public void listenReceiveMessage(final String message) {
        log.debug("Receive message from Domibus : {}", message);
        apIncomingService.manageIncomingNotification(
                serializeUtils.mapJsonStringToClass(message, ReceivedNotificationDto.class));
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.messageReceiveDeadLetterQueue:messageReceiveDeadLetterQueue}")
    public void listenMessageReceiveDeadQueue(final String message) {
        log.error("Receive message from dead queue : {}", message);
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.eftiSendMessageQueue:efti.send-messages.q}")
    public void listenSendMessage(final String message) {

        log.info("receive message from rabbimq queue");
        trySendDomibus(serializeUtils.mapJsonStringToClass(message, RabbitRequestDto.class));
    }

    private void trySendDomibus(final RabbitRequestDto rabbitRequestDto) {

        final RequestTypeEnum requestTypeEnum = rabbitRequestDto.getControl().getRequestType();
        final ComponentType target = gateProperties.isCurrentGate(rabbitRequestDto.getGateIdDest()) ? ComponentType.PLATFORM : ComponentType.GATE;

        if (ComponentType.PLATFORM.equals(target) && platformIntegrationService.getPlatformInfo(rabbitRequestDto.getControl().getPlatformId()).map(PlatformInfo::useRestApi).orElse(false)) {
            var platformId = rabbitRequestDto.getControl().getPlatformId();
            var platformInfo = platformIntegrationService.getPlatformInfo(platformId);
            if (platformInfo.isEmpty()) {
                throw new IllegalArgumentException("platform " + platformId + " does not exist");
            }
            try {
                uilRequestService.manageRestRequestInProgress(rabbitRequestDto.getControl().getRequestId());
                var res = platformIntegrationService.callGetConsignmentSubsets(platformId, rabbitRequestDto.getControl().getDatasetId(), Set.copyOf(rabbitRequestDto.getControl().getSubsetIds()));
                uilRequestService.manageRestResponseReceived(rabbitRequestDto.getControl().getRequestId(), res);
            } catch (PlatformIntegrationServiceException e) {
                throw new RuntimeException(e);
            }
        } else {
            final RequestDto requestDto = mapperUtils.rabbitRequestDtoToRequestDto(rabbitRequestDto, EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(rabbitRequestDto.getRequestType()));
            String previousEdeliveryMessageId = rabbitRequestDto.getEdeliveryMessageId();
            try {
                String eDeliveryMessageId = messageIdGenerator.generateMessageId();
                if (rabbitRequestDto.getError() == null || !ErrorCodesEnum.REQUESTID_MISSING.name().equals(rabbitRequestDto.getError().getErrorCode())) {
                    getRequestService(rabbitRequestDto.getRequestType()).updateRequestStatus(requestDto, eDeliveryMessageId);
                }
                this.requestSendingService.sendRequest(buildApRequestDto(rabbitRequestDto, eDeliveryMessageId));
            } catch (final SendRequestException e) {
                log.error("error while sending request" + e);
                getRequestService(rabbitRequestDto.getRequestType()).updateRequestStatus(requestDto, previousEdeliveryMessageId);
                throw new TechnicalException("Error when try to send message to domibus", e);
            } finally {
                final String body = getRequestService(requestTypeEnum).buildRequestBody(rabbitRequestDto);
                final String receiver = ComponentType.PLATFORM.equals(target) ? rabbitRequestDto.getControl().getPlatformId() : rabbitRequestDto.getGateIdDest();
                if (RequestType.UIL.equals(requestDto.getRequestType())) {
                    //log fti020 and fti009
                    logManager.logSentMessage(requestDto.getControl(), body, receiver, ComponentType.GATE, target, true, LogManager.FTI_009_FTI_020);
                } else if (RequestType.IDENTIFIER.equals(requestDto.getRequestType())) {
                    //log fti019
                    logManager.logSentMessage(requestDto.getControl(), body, receiver, ComponentType.GATE, ComponentType.GATE, true, LogManager.FTI_019);
                }
            }
        }
    }

    private ApRequestDto buildApRequestDto(final RabbitRequestDto requestDto, String edeliveryMessageId) {
        final String receiver = gateProperties.isCurrentGate(requestDto.getGateIdDest()) ? requestDto.getControl().getPlatformId() : requestDto.getGateIdDest();
        return ApRequestDto.builder()
                .requestId(requestDto.getControl().getRequestId())
                .sender(gateProperties.getOwner()).receiver(receiver)
                .body(getRequestService(requestDto.getRequestType()).buildRequestBody(requestDto))
                .eDeliveryMessageId(edeliveryMessageId)
                .apConfig(ApConfigDto.builder()
                        .username(gateProperties.getAp().getUsername())
                        .password(gateProperties.getAp().getPassword())
                        .url(gateProperties.getAp().getUrl())
                        .build())
                .build();
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.messageSendDeadLetterQueue:message-send-dead-letter-queue}")
    public void listenSendMessageDeadLetter(final String message) {
        log.error("Receive message for dead queue");
        final RequestDto requestDto = serializeUtils.mapJsonStringToClass(message, RequestDto.class);
        this.getRequestService(requestDto.getControl().getRequestType()).manageSendError(requestDto);
    }

    private RequestService<?> getRequestService(final RequestType requestType) {
        return requestServiceFactory.getRequestServiceByRequestType(requestType.name());
    }

    private RequestService<?> getRequestService(final RequestTypeEnum requestType) {
        return requestServiceFactory.getRequestServiceByRequestType(requestType);
    }
}
