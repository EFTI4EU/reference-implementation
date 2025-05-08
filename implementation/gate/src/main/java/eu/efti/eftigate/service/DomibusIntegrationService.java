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
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.generator.id.MessageIdGenerator;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftilogger.model.ComponentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class DomibusIntegrationService {

    private final GateProperties gateProperties;
    private final SerializeUtils serializeUtils;
    private final RequestSendingService requestSendingService;
    private final RequestServiceFactory requestServiceFactory;
    private final MapperUtils mapperUtils;
    private final LogManager logManager;
    private final MessageIdGenerator messageIdGenerator;

    void trySendDomibus(final RabbitRequestDto rabbitRequestDto, RequestTypeEnum requestTypeEnum, String receiverLabel) {
        final ComponentType target = gateProperties.isCurrentGate(rabbitRequestDto.getGateIdDest()) ? ComponentType.PLATFORM : ComponentType.GATE;

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
            if (RequestType.UIL.equals(requestDto.getRequestType())) {
                //log fti020 and fti009
                logManager.logSentMessage(requestDto.getControl(), body, receiverLabel, ComponentType.GATE, target, true, LogManager.FTI_009_FTI_020);
            } else if (RequestType.IDENTIFIER.equals(requestDto.getRequestType())) {
                //log fti019
                logManager.logSentMessage(requestDto.getControl(), body, receiverLabel, ComponentType.GATE, ComponentType.GATE, true, LogManager.FTI_019);
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
