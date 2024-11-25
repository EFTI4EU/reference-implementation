package eu.efti.eftigate.service;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.dto.RequestDto;
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
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
        final boolean isCurrentGate = gateProperties.isCurrentGate(rabbitRequestDto.getGateIdDest());
        final String receiver = isCurrentGate ? rabbitRequestDto.getControl().getPlatformId() : rabbitRequestDto.getGateIdDest();
        final RequestDto requestDto = mapperUtils.rabbitRequestDtoToRequestDto(rabbitRequestDto, EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(rabbitRequestDto.getRequestType()));
        boolean hasBeenSent = false;

        try {
            final String edeliveryMessageId = this.requestSendingService.sendRequest(buildApRequestDto(rabbitRequestDto));
            getRequestService(rabbitRequestDto.getRequestType()).updateSentRequestStatus(requestDto, edeliveryMessageId);
            hasBeenSent = true;
        } catch (final SendRequestException e) {
            log.error("error while sending request" + e);
            throw new TechnicalException("Error when try to send message to domibus", e);
        } finally {
            final String body = getRequestService(requestTypeEnum).buildRequestBody(rabbitRequestDto);
            if (RequestType.UIL.equals(requestDto.getRequestType())) {
                //log fti020 and fti009
                logManager.logSentMessage(requestDto.getControl(), body, receiver, isCurrentGate, hasBeenSent, LogManager.UIL_FTI_020_FTI_009);
            } else if (RequestType.IDENTIFIER.equals(requestDto.getRequestType())) {
                //log fti019
                logManager.logRequestForIdentifiers(requestDto.getControl(), body, gateProperties.getOwner(), gateProperties.getCountry(), requestDto.getError() != null ? requestDto.getError().getErrorCode() : null, LogManager.IDENTIFIERS);
            }
        }
    }

    private ApRequestDto buildApRequestDto(final RabbitRequestDto requestDto) {
        final String receiver = gateProperties.isCurrentGate(requestDto.getGateIdDest()) ? requestDto.getControl().getPlatformId() : requestDto.getGateIdDest();
        return ApRequestDto.builder()
                .requestId(requestDto.getControl().getRequestId())
                .sender(gateProperties.getOwner()).receiver(receiver)
                .body(getRequestService(requestDto.getRequestType()).buildRequestBody(requestDto))
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
