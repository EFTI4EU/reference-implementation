package com.ingroupe.efti.eftigate.service.request;

import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.ErrorDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.dto.UilRequestDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.ErrorCodesEnum;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.commons.utils.SerializeUtils;
import com.ingroupe.efti.edeliveryapconnector.dto.MessageBodyDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.service.RequestUpdaterService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.dto.RabbitRequestDto;
import com.ingroupe.efti.eftigate.dto.requestbody.RequestBodyDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.ErrorEntity;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.entity.UilRequestEntity;
import com.ingroupe.efti.eftigate.exception.RequestNotFoundException;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.repository.UilRequestRepository;
import com.ingroupe.efti.eftigate.service.ControlService;
import com.ingroupe.efti.eftigate.service.LogManager;
import com.ingroupe.efti.eftigate.service.RabbitSenderService;
import com.ingroupe.efti.eftigate.utils.ControlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.ingroupe.efti.commons.constant.EftiGateConstants.UIL_ACTIONS;
import static com.ingroupe.efti.commons.constant.EftiGateConstants.UIL_TYPES;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;

@Slf4j
@Component
public class UilRequestService extends RequestService<UilRequestEntity> {

    private static final String UIL = "UIL";
    private final UilRequestRepository uilRequestRepository;

    public UilRequestService(final UilRequestRepository uilRequestRepository, final MapperUtils mapperUtils,
                             final RabbitSenderService rabbitSenderService,
                             final ControlService controlService,
                             final GateProperties gateProperties,
                             final RequestUpdaterService requestUpdaterService,
                             final SerializeUtils serializeUtils,
                             final LogManager logManager) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.uilRequestRepository = uilRequestRepository;
    }


    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(UilRequestEntity.class::isInstance)
                .map(UilRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getReponseData()));
    }

    @Override
    public void setDataFromRequests(final ControlEntity controlEntity) {
        controlEntity.setEftiData(controlEntity.getRequests().stream()
                .map(UilRequestEntity.class::cast)
                .map(UilRequestEntity::getReponseData).toList().stream()
                .collect(ByteArrayOutputStream::new, (byteArrayOutputStream, bytes) -> byteArrayOutputStream.write(bytes, 0, bytes.length), (arrayOutputStream, byteArrayOutputStream) -> {})
                .toByteArray());
    }

    @Override
    public void manageMessageReceive(final NotificationDto notificationDto) {
        final MessageBodyDto messageBody =
                getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), MessageBodyDto.class);

        final UilRequestEntity uilRequestEntity = this.findByRequestUuidOrThrow(messageBody.getRequestUuid());
        if (messageBody.getStatus().equals(StatusEnum.COMPLETE.name())) {
            uilRequestEntity.setReponseData(messageBody.getEFTIData().toString().getBytes(StandardCharsets.UTF_8));
            this.updateStatus(uilRequestEntity, RequestStatusEnum.SUCCESS, notificationDto.getMessageId());
        } else {
            this.updateStatus(uilRequestEntity, RequestStatusEnum.ERROR, notificationDto.getMessageId());
            errorReceived(uilRequestEntity, messageBody.getErrorDescription());
        }
        final ControlDto controlDto = getMapperUtils().controlEntityToControlDto(uilRequestEntity.getControl());
        getLogManager().logReceivedMessage(controlDto, notificationDto.getContent().getBody(), notificationDto.getContent().getFromPartyId());
        responseToOtherGateIfNecessary(uilRequestEntity);
    }

    @Override
    public void manageSendSuccess(final String eDeliveryMessageId) {
        final UilRequestEntity externalRequest = uilRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(EXTERNAL_ASK_UIL_SEARCH,
                RESPONSE_IN_PROGRESS, eDeliveryMessageId);
        if (externalRequest == null) {
            log.info(" sent message {} successfully", eDeliveryMessageId);
        } else {
            externalRequest.getControl().setStatus(StatusEnum.COMPLETE);
            this.updateStatus(externalRequest, SUCCESS);
        }
    }

    public void updateStatus(final UilRequestEntity requestEntity, final RequestStatusEnum status, final String eDeliveryMessageId) {
        this.updateStatus(requestEntity, status);
        markMessageAsDownloaded(eDeliveryMessageId);
    }

    protected void errorReceived(final UilRequestEntity requestEntity, final String errorDescription) {
        log.error("Error received, change status of requestId : {}", requestEntity.getControl().getRequestUuid());
        final ErrorEntity errorEntity = ErrorEntity.builder()
                .errorDescription(errorDescription)
                .errorCode(ErrorCodesEnum.PLATFORM_ERROR.toString())
                .build();

        final ControlEntity controlEntity = requestEntity.getControl();
        controlEntity.setError(errorEntity);
        controlEntity.setStatus(StatusEnum.ERROR);

        requestEntity.setControl(controlEntity);
        uilRequestRepository.save(requestEntity);
        getControlService().save(controlEntity);
    }

    private void responseToOtherGateIfNecessary(final UilRequestEntity requestEntity) {
        if (!requestEntity.getControl().isExternalAsk()) return;
        this.updateStatus(requestEntity, RESPONSE_IN_PROGRESS);
        requestEntity.setGateUrlDest(requestEntity.getControl().getFromGateUrl());
        requestEntity.getControl().setEftiData(requestEntity.getReponseData());
        getControlService().save(requestEntity.getControl());
        final UilRequestDto requestDto = getMapperUtils().requestToRequestDto(requestEntity, UilRequestDto.class);
        requestDto.setRequestType(RequestType.UIL);
        this.sendRequest(requestDto);
    }

    private UilRequestEntity findByRequestUuidOrThrow(final String requestId) {
        return Optional.ofNullable(
                        this.uilRequestRepository.findByControlRequestUuidAndStatus(requestId, RequestStatusEnum.IN_PROGRESS))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find request for requestUuid: " + requestId));
    }

    @Override
    public void receiveGateRequest(final NotificationDto notificationDto) {
        final MessageBodyDto messageBody = getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), MessageBodyDto.class);

        final UilRequestEntity requestEntity = uilRequestRepository
                .findByControlRequestUuidAndStatus(messageBody.getRequestUuid(), RequestStatusEnum.IN_PROGRESS);

        final ControlDto controlDto = requestEntity != null ? manageResponseFromOtherGate(requestEntity, messageBody) :
                this.getControlService().createUilControl(ControlUtils
                    .fromGateToGateMessageBodyDto(messageBody, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH,
                            notificationDto, getGateProperties().getOwner()));

        getLogManager().logReceivedMessage(controlDto, notificationDto.getContent().getBody(), notificationDto.getContent().getFromPartyId());
    }

    private ErrorEntity setErrorFromMessageBodyDto(final MessageBodyDto messageBody) {
        return StringUtils.isBlank(messageBody.getErrorDescription()) ?
                getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND))
                :
                getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromAnyError(messageBody.getErrorDescription()));
    }

    @Override
    public UilRequestDto createRequest(final ControlDto controlDto) {
        return new UilRequestDto(controlDto);
    }

    @Override
    public String buildRequestBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        if (requestDto.getStatus() == RESPONSE_IN_PROGRESS || requestDto.getStatus() == RequestStatusEnum.ERROR) {
            final boolean hasData = requestDto.getReponseData() != null;
            final boolean hasError = controlDto.getError() != null;

            return getSerializeUtils().mapObjectToXmlString(MessageBodyDto.builder()
                    .requestUuid(controlDto.getRequestUuid())
                    .eFTIData(hasData ? new String(requestDto.getReponseData()) : null)
                    .status(hasError ? StatusEnum.ERROR.name() : StatusEnum.COMPLETE.name())
                    .errorDescription(hasError ? controlDto.getError().getErrorDescription() : null)
                    .eFTIDataUuid(controlDto.getEftiDataUuid())
                    .build());
        }

        final RequestBodyDto requestBodyDto = RequestBodyDto.builder()
                .eFTIData(requestDto.getReponseData() != null ? new String(requestDto.getReponseData(), StandardCharsets.UTF_8) : null)
                .eFTIPlatformUrl(requestDto.getControl().getEftiPlatformUrl())
                .requestUuid(controlDto.getRequestUuid())
                .eFTIDataUuid(controlDto.getEftiDataUuid())
                .subsetEU(new LinkedList<>())
                .subsetMS(new LinkedList<>())
                .build();
        return getSerializeUtils().mapObjectToXmlString(requestBodyDto);
    }

    @Override
    public RequestDto save(final RequestDto requestDto) {
        return getMapperUtils().requestToRequestDto(
                uilRequestRepository.save(getMapperUtils().requestDtoToRequestEntity(requestDto, UilRequestEntity.class)),
                UilRequestDto.class);
    }

    @Override
    protected void updateStatus(final UilRequestEntity uilRequestEntity, final RequestStatusEnum status) {
        uilRequestEntity.setStatus(status);
        getControlService().save(uilRequestEntity.getControl());
        uilRequestRepository.save(uilRequestEntity);
    }

    @Override
    protected UilRequestEntity findRequestByMessageIdOrThrow(final String eDeliveryMessageId) {
        return Optional.ofNullable(this.uilRequestRepository.findByEdeliveryMessageId(eDeliveryMessageId))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find Uil request for messageId: " + eDeliveryMessageId));
    }

    private ControlDto manageResponseFromOtherGate(final UilRequestEntity requestEntity, final MessageBodyDto messageBody) {
        if (!ObjectUtils.isEmpty(messageBody.getEFTIData())) {
            requestEntity.setReponseData(messageBody.getEFTIData().toString().getBytes(StandardCharsets.UTF_8));
            requestEntity.setStatus(RequestStatusEnum.SUCCESS);
        } else {
            requestEntity.setStatus(RequestStatusEnum.ERROR);
            requestEntity.setError(setErrorFromMessageBodyDto(messageBody));
            final ControlEntity controlEntity = requestEntity.getControl();
            controlEntity.setError(setErrorFromMessageBodyDto(messageBody));
            controlEntity.setStatus(StatusEnum.ERROR);
            requestEntity.setControl(controlEntity);
        }
        uilRequestRepository.save(requestEntity);
        return getControlService().save(requestEntity.getControl());
    }

    @Override
    public boolean supports(final RequestTypeEnum requestTypeEnum) {
        return UIL_TYPES.contains(requestTypeEnum);
    }

    @Override
    public boolean supports(final EDeliveryAction eDeliveryAction) {
        return UIL_ACTIONS.contains(eDeliveryAction);
    }

    @Override
    public boolean supports(final String requestType) {
        return UIL.equalsIgnoreCase(requestType);
    }
}
