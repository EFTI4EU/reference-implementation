package eu.efti.eftigate.service;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
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
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.model.RequestTypeLog;
import eu.efti.eftilogger.service.ReportingRequestLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;

import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.PLATFORM;

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
    private final ReportingRequestLogService reportingRequestLogService;
    private final MessageIdGenerator messageIdGenerator;
    private final EftiGateIdResolver eftiGateIdResolver;


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
        String previousEdeliveryMessageId = rabbitRequestDto.getEdeliveryMessageId();
        try {
            String eDeliveryMessageId = messageIdGenerator.generateMessageId();
            ErrorDto errorDto = rabbitRequestDto.getError();
            if (errorDto == null || !ErrorCodesEnum.REQUESTID_MISSING.name().equals(errorDto.getErrorCode())) {
                getRequestService(rabbitRequestDto.getRequestType()).updateRequestStatus(requestDto, eDeliveryMessageId);
            }
            this.requestSendingService.sendRequest(buildApRequestDto(rabbitRequestDto, eDeliveryMessageId));
            requestDto.setSentDate(OffsetDateTime.now());
            getRequestService(requestDto.getRequestType()).save(requestDto);
        } catch (final SendRequestException e) {
            log.error("error while sending request" + e);
            getRequestService(rabbitRequestDto.getRequestType()).updateRequestStatus(requestDto, previousEdeliveryMessageId);
            throw new TechnicalException("Error when try to send message to domibus", e);
        } finally {
            logSentMessage(rabbitRequestDto, requestTypeEnum, requestDto, receiver);
            ControlDto controlDto = requestDto.getControl();
            if (RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH.equals(controlDto.getRequestType()) && !gateProperties.isCurrentGate(requestDto.getGateIdDest()) && !RequestType.NOTE.equals(requestDto.getRequestType())) {
                reportingRequestLogService.logReportingRequest(controlDto, requestDto, gateProperties.getOwner(), gateProperties.getCountry(), RequestTypeLog.UIL_ACK, GATE, controlDto.getFromGateId(), eftiGateIdResolver.resolve(controlDto.getFromGateId()), GATE, gateProperties.getOwner(), gateProperties.getCountry(), false);
            }
        }
    }

    private void logSentMessage(final RabbitRequestDto rabbitRequestDto, final RequestTypeEnum requestTypeEnum, final RequestDto requestDto, final String receiver) {
        final String body = getRequestService(requestDto.getRequestType()).buildRequestBody(rabbitRequestDto);
        ControlDto controlDto = requestDto.getControl();
        if (RequestType.UIL.equals(requestDto.getRequestType())) {
            logSentUilMessage(rabbitRequestDto, controlDto, receiver, body);
        } else if (RequestType.IDENTIFIER.equals(requestDto.getRequestType())) {
            logSentIdentifierMessage(requestTypeEnum, controlDto, receiver, body);
        } else if (RequestType.NOTE.equals(requestDto.getRequestType())) {
            logSentNoteMessage(controlDto, receiver, body);
        }
    }

    private void logSentNoteMessage(final ControlDto control, final String receiver, final String body) {
        final boolean isCurrentGate = gateProperties.isCurrentGate(control.getGateId());

        String logName = isCurrentGate ? LogManager.FTI_025 : LogManager.FTI_026;
        logManager.logReceivedNote(control, body, receiver, ComponentType.GATE, LogManager.FTI_025.equalsIgnoreCase(logName) ? PLATFORM : GATE,
                true, logName);
    }

    private void logSentIdentifierMessage(final RequestTypeEnum requestTypeEnum, final ControlDto controlDto, final String receiver, final String body) {
        //log fti019 or fti021
        if (RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH.equals(requestTypeEnum)) {
            logManager.logSentMessage(controlDto, body, receiver, GATE, GATE, true, LogManager.FTI_021);
        } else {
            logManager.logSentMessage(controlDto, body, receiver, GATE, GATE, true, LogManager.FTI_019);
        }
    }

    private void logSentUilMessage(final RabbitRequestDto rabbitRequestDto, final ControlDto controlDto, final String receiver, final String body) {
        //log fti020 and fti009
        if (StringUtils.isNotBlank(receiver) && receiver.equalsIgnoreCase(rabbitRequestDto.getControl().getPlatformId())) {
            logManager.logSentMessage(controlDto, body, receiver, GATE, PLATFORM, true, LogManager.FTI_009);
        } else if (RequestTypeEnum.EXTERNAL_UIL_SEARCH.equals(controlDto.getRequestType())) {
            logManager.logSentMessage(controlDto, body, receiver, GATE, GATE, true, LogManager.FTI_020);
        }
    }

    private ApRequestDto buildApRequestDto(final RabbitRequestDto requestDto, final String edeliveryMessageId) {
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
