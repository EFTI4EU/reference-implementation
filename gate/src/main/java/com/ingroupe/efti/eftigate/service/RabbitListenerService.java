package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.constant.EftiGateConstants;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.exception.TechnicalException;
import com.ingroupe.efti.commons.utils.SerializeUtils;
import com.ingroupe.efti.edeliveryapconnector.dto.ApConfigDto;
import com.ingroupe.efti.edeliveryapconnector.dto.ApRequestDto;
import com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import com.ingroupe.efti.edeliveryapconnector.exception.SendRequestException;
import com.ingroupe.efti.edeliveryapconnector.service.RequestSendingService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.dto.RabbitRequestDto;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.service.request.RequestService;
import com.ingroupe.efti.eftigate.service.request.RequestServiceFactory;
import com.ingroupe.efti.eftilogger.dto.LogRegistryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class RabbitListenerService {

    private final GateProperties gateProperties;
    private final SerializeUtils serializeUtils;
    private final RequestSendingService requestSendingService;
    private final RequestServiceFactory requestServiceFactory;
    private final ApIncomingService apIncomingService;
    private final Function<RabbitRequestDto, EDeliveryAction> requestToEDeliveryActionFunction;
    private final MapperUtils mapperUtils;
    private final LogManager logManager;


    @RabbitListener(queues = "${spring.rabbitmq.queues.eftiReceiveMessageQueue:efti.receive-messages.q}")
    public void listenReceiveMessage(final String message) {
        log.info("Receive message from Domibus : {}", message);
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

        final EDeliveryAction eDeliveryAction = requestToEDeliveryActionFunction.apply(rabbitRequestDto);
        final boolean isCurrentGate = gateProperties.isCurrentGate(rabbitRequestDto.getGateUrlDest());
        final String receiver = isCurrentGate ? rabbitRequestDto.getControl().getEftiPlatformUrl() : rabbitRequestDto.getGateUrlDest();
        final RequestDto requestDto = mapperUtils.rabbitRequestDtoToRequestDto(rabbitRequestDto, EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(rabbitRequestDto.getRequestType()));
        boolean hasBeenSent = false;

        try {
            final String edeliveryMessageId = this.requestSendingService.sendRequest(buildApRequestDto(rabbitRequestDto, eDeliveryAction), eDeliveryAction);
            getRequestService(eDeliveryAction).updateSentRequestStatus(requestDto, edeliveryMessageId);
            hasBeenSent = true;
        } catch (final SendRequestException e) {
            log.error("error while sending request" + e);
            throw new TechnicalException("Error when try to send message to domibus", e);
        } finally {
            final String body = getRequestService(eDeliveryAction).buildRequestBody(rabbitRequestDto);
            if (RequestType.UIL.equals(requestDto.getRequestType())) {
                //log fti020 and fti009
                logManager.logSentMessage(requestDto.getControl(), body, receiver, isCurrentGate, hasBeenSent, LogManager.UIL_FTI_020_FTI_009);
            } else if (RequestType.IDENTIFIER.equals(requestDto.getRequestType())) {
                //log fti019
                logManager.logRequestForMetadata(requestDto.getControl(), body, gateProperties.getOwner(), gateProperties.getCountry(), requestDto.getError() != null  ? requestDto.getError().getErrorCode() : null, LogManager.METADATA);
            }
        }
    }

    private ApRequestDto buildApRequestDto(final RabbitRequestDto requestDto, final EDeliveryAction eDeliveryAction) {
        final String receiver = gateProperties.isCurrentGate(requestDto.getGateUrlDest()) ? requestDto.getControl().getEftiPlatformUrl() : requestDto.getGateUrlDest();
        return ApRequestDto.builder()
                .sender(gateProperties.getOwner()).receiver(receiver)
                .body(getRequestService(eDeliveryAction).buildRequestBody(requestDto))
                .apConfig(ApConfigDto.builder()
                        .username(gateProperties.getAp().getUsername())
                        .password(gateProperties.getAp().getPassword())
                        .url(gateProperties.getAp().getUrl())
                        .build())
                .build();
    }

    private RequestService<?> getRequestService(final EDeliveryAction eDeliveryAction) {
        return  requestServiceFactory.getRequestServiceByEdeliveryActionType(eDeliveryAction);
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.messageSendDeadLetterQueue:message-send-dead-letter-queue}")
    public void listenSendMessageDeadLetter(final String message) {
        log.error("Receive message for dead queue");
        final RequestDto requestDto = serializeUtils.mapJsonStringToClass(message, RequestDto.class);
        this.getRequestService(requestDto.getControl().getRequestType()).manageSendError(requestDto);
    }

    private RequestService<?> getRequestService(final RequestTypeEnum requestType) {
        return requestServiceFactory.getRequestServiceByRequestType(requestType);
    }
}
