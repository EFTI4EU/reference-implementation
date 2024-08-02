package com.ingroupe.efti.eftigate.service.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.ErrorDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.ErrorCodesEnum;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.utils.SerializeUtils;
import com.ingroupe.efti.edeliveryapconnector.dto.ApConfigDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationType;
import com.ingroupe.efti.edeliveryapconnector.service.RequestUpdaterService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.dto.RabbitRequestDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.service.ControlService;
import com.ingroupe.efti.eftigate.service.LogManager;
import com.ingroupe.efti.eftigate.service.RabbitSenderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import static com.ingroupe.efti.commons.constant.EftiGateConstants.EXTERNAL_REQUESTS_TYPES;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.ERROR;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.SEND_ERROR;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.TIMEOUT;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Getter
@Transactional("controlTransactionManager")
public abstract class RequestService<T extends RequestEntity> {
    private final MapperUtils mapperUtils;
    private final RabbitSenderService rabbitSenderService;
    @Lazy
    private final ControlService controlService;
    private final GateProperties gateProperties;
    private final RequestUpdaterService requestUpdaterService;
    private final SerializeUtils serializeUtils;
    private final LogManager logManager;

    @Value("${spring.rabbitmq.queues.eftiSendMessageExchange:efti.send-message.exchange}")
    private String eftiSendMessageExchange;
    @Value("${spring.rabbitmq.queues.eftiKeySendMessage:EFTI}")
    private String eftiKeySendMessage;

    public abstract boolean allRequestsContainsData(List<RequestEntity> controlEntityRequests);

    public abstract void setDataFromRequests(ControlEntity controlEntity);

    public abstract void manageMessageReceive(final NotificationDto notificationDto);

    public abstract void manageSendSuccess(final String eDeliveryMessageId);

    public abstract boolean supports(final RequestTypeEnum requestTypeEnum);

    public abstract boolean supports(final EDeliveryAction eDeliveryAction);

    public abstract boolean supports(final String requestType);

    public abstract void receiveGateRequest(final NotificationDto notificationDto);

    public abstract RequestDto createRequest(final ControlDto controlDto);

    public abstract String buildRequestBody(final RabbitRequestDto rabbitRequestDto);

    public abstract RequestDto save(final RequestDto requestDto);

    protected abstract void updateStatus(final T requestEntity, final RequestStatusEnum status);

    protected abstract T findRequestByMessageIdOrThrow(final String eDeliveryMessageId);

    public void manageSendFailure(final NotificationDto notificationDto) {
        this.updateStatus(findRequestByMessageIdOrThrow(notificationDto.getMessageId()), SEND_ERROR);
    }

    public void createAndSendRequest(final ControlDto controlDto, final String destinationUrl){
        this.createAndSendRequest(controlDto, destinationUrl, RequestStatusEnum.RECEIVED);
    }

    public void createAndSendRequest(final ControlDto controlDto, final String destinationUrl, final RequestStatusEnum status) {
        final RequestDto requestDto = initRequest(controlDto, destinationUrl);
        requestDto.setStatus(status);
        final RequestDto result = this.save(requestDto);
        this.sendRequest(result);
    }

    protected RequestDto initRequest(final ControlDto controlDto, final String destinationUrl) {
        final RequestDto requestDto = createRequest(controlDto);
        requestDto.setGateUrlDest(StringUtils.isNotBlank(destinationUrl) ? destinationUrl : controlDto.getEftiGateUrl());
        log.info("Request has been register with controlId : {}", requestDto.getControl().getId());
        return requestDto;
    }

    public void sendRequest(final RequestDto requestDto) {
        try {
            rabbitSenderService.sendMessageToRabbit(eftiSendMessageExchange, eftiKeySendMessage, requestDto);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to parse object to json/string", e);
        }
    }

    public void updateWithResponse(final NotificationDto notificationDto) {
        if (Objects.requireNonNull(notificationDto.getNotificationType()) == NotificationType.RECEIVED) {
            manageMessageReceive(notificationDto);
        } else {
            log.warn("unknown notification {} ", notificationDto.getNotificationType());
        }
    }

    protected String getRequestUuid(final String bodyXmlString) {
        final XPathFactory xpathFactory = XPathFactory.newInstance();
        final XPath xpath = xpathFactory.newXPath();
        final InputSource xml = new InputSource(new StringReader(bodyXmlString));
        try {
            return xpath.evaluate("/body/requestUuid", xml);
        } catch (final XPathExpressionException e) {
            return null;
        }
    }

    public <R extends RequestDto> RequestDto updateStatus(final R requestDto, final RequestStatusEnum status) {
        requestDto.setStatus(status);
        return this.save(requestDto);
    }

    public void manageSendError(final RequestDto requestDto) {
        final ErrorDto errorDto = ErrorDto.fromErrorCode(ErrorCodesEnum.AP_SUBMISSION_ERROR);
        requestDto.setError(errorDto);
        controlService.setError(requestDto.getControl(), errorDto);
        final RequestDto requestDtoUpdated = this.updateStatus(requestDto, ERROR);
        if (requestDtoUpdated.getControl().getFromGateUrl() != null &&
                !gateProperties.isCurrentGate(requestDtoUpdated.getControl().getFromGateUrl()) &&
                ErrorCodesEnum.AP_SUBMISSION_ERROR.name().equals(requestDto.getControl().getError().getErrorCode())) {
            requestDtoUpdated.setGateUrlDest(requestDtoUpdated.getControl().getFromGateUrl());
            requestDtoUpdated.getControl().setEftiGateUrl(requestDtoUpdated.getControl().getFromGateUrl());
            this.sendRequest(requestDtoUpdated);
        }
    }

    public void createRequest(final ControlDto controlDto, final RequestStatusEnum status) {
        final RequestDto requestDto = save(buildRequestDto(controlDto, status));
        log.info("Request has been registered with controlId : {}", requestDto.getControl().getId());
    }

    public void updateSentRequestStatus(final RequestDto requestDto, final String edeliveryMessageId) {
        requestDto.setEdeliveryMessageId(edeliveryMessageId);
        final RequestStatusEnum requestStatus = requestDto.getStatus();
        if (!(RESPONSE_IN_PROGRESS.equals(requestStatus) || ERROR.equals(requestStatus) || TIMEOUT.equals(requestStatus))){
            requestDto.setStatus(IN_PROGRESS);
        }
        this.save(requestDto);
    }

    protected void markMessageAsDownloaded(final String eDeliveryMessageId){
        try {
            getRequestUpdaterService().setMarkedAsDownload(createApConfig(), eDeliveryMessageId);
        } catch (final MalformedURLException e) {
            log.error("Error while try to set mark as download", e);
        }
    }

    private ApConfigDto createApConfig() {
        return ApConfigDto.builder()
                .username(getGateProperties().getAp().getUsername())
                .password(getGateProperties().getAp().getPassword())
                .url(getGateProperties().getAp().getUrl())
                .build();
    }

    protected boolean isExternalRequest(final RequestDto requestDto) {
        return EXTERNAL_REQUESTS_TYPES.contains(requestDto.getControl().getRequestType());
    }

    private RequestDto buildRequestDto(final ControlDto controlDto, final RequestStatusEnum status) {
        return RequestDto.builder()
                .retry(0)
                .control(controlDto)
                .status(status)
                .gateUrlDest(controlDto.getFromGateUrl())
                .build();
    }

    public void notifyTimeout(final RequestDto requestDto) {
        requestDto.setGateUrlDest(requestDto.getControl().getFromGateUrl());
        sendRequest(requestDto);
    }
}
