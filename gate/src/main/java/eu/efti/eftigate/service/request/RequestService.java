package eu.efti.eftigate.service.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.v1.edelivery.ObjectFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.List;

import static eu.efti.commons.constant.EftiGateConstants.EXTERNAL_REQUESTS_TYPES;
import static eu.efti.commons.enums.RequestStatusEnum.ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.TIMEOUT;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Getter
public abstract class RequestService<T extends RequestEntity> {
    private final MapperUtils mapperUtils;
    private final RabbitSenderService rabbitSenderService;
    @Lazy
    private final ControlService controlService;
    private final GateProperties gateProperties;
    private final RequestUpdaterService requestUpdaterService;
    private final SerializeUtils serializeUtils;
    private final LogManager logManager;
    private final ObjectFactory objectFactory = new ObjectFactory();

    @Value("${spring.rabbitmq.queues.eftiSendMessageExchange:efti.send-message.exchange}")
    private String eftiSendMessageExchange;
    @Value("${spring.rabbitmq.queues.eftiKeySendMessage:EFTI}")
    private String eftiKeySendMessage;

    public abstract boolean allRequestsContainsData(List<RequestEntity> controlEntityRequests);

    public abstract void manageSendSuccess(final String eDeliveryMessageId);

    public abstract boolean supports(final RequestTypeEnum requestTypeEnum);

    public abstract boolean supports(final String requestType);

    public abstract RequestDto createRequest(final ControlDto controlDto);

    public abstract String buildRequestBody(final RabbitRequestDto rabbitRequestDto);

    public abstract RequestDto save(final RequestDto requestDto);

    protected abstract void updateStatus(final T requestEntity, final RequestStatusEnum status);

    protected abstract T findRequestByMessageIdOrThrow(final String eDeliveryMessageId);

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
