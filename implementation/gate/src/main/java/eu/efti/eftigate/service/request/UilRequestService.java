package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.UilRequestDto;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.MessageBodyDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.dto.requestbody.RequestBodyDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.ErrorEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.UilRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.eftigate.utils.ControlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.UIL_ACTIONS;
import static eu.efti.commons.constant.EftiGateConstants.UIL_TYPES;
import static eu.efti.commons.enums.RequestStatusEnum.ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.RequestStatusEnum.TIMEOUT;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;

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
    public void manageMessageReceive(final NotificationDto notificationDto) {
        final MessageBodyDto messageBody =
                getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), MessageBodyDto.class);

        final UilRequestEntity uilRequestEntity = this.findByRequestUuidOrThrow(messageBody.getRequestUuid());
        if (messageBody.getStatus().equals(StatusEnum.COMPLETE.name())) {
            uilRequestEntity.setReponseData(messageBody.getEFTIData().toString().getBytes(StandardCharsets.UTF_8));
            this.updateStatus(uilRequestEntity, RequestStatusEnum.SUCCESS, notificationDto.getMessageId());
        } else {
            this.updateStatus(uilRequestEntity, ERROR, notificationDto.getMessageId());
            errorReceived(uilRequestEntity, messageBody.getErrorDescription());
        }
        final ControlDto controlDto = getMapperUtils().controlEntityToControlDto(uilRequestEntity.getControl());
        getLogManager().logReceivedMessage(controlDto, notificationDto.getContent().getBody(), notificationDto.getContent().getFromPartyId(), LogManager.FTI_010_FTI_022_ET_AUTRES);
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

    @Override
    public void receiveGateRequest(final NotificationDto notificationDto) {
        final MessageBodyDto messageBody = getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), MessageBodyDto.class);

        final UilRequestEntity requestEntity = uilRequestRepository
                .findByControlRequestUuidAndStatus(messageBody.getRequestUuid(), RequestStatusEnum.IN_PROGRESS);

        final ControlDto controlDto = requestEntity != null ? manageResponseFromOtherGate(requestEntity, messageBody) :
                this.getControlService().createUilControl(ControlUtils
                        .fromGateToGateMessageBodyDto(messageBody, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH,
                                notificationDto, getGateProperties().getOwner()));
        //log efti022
        getLogManager().logReceivedMessage(controlDto, notificationDto.getContent().getBody(), notificationDto.getContent().getFromPartyId(), LogManager.FTI_022_FTI_010);
    }

    @Override
    public UilRequestDto createRequest(final ControlDto controlDto) {
        return new UilRequestDto(controlDto);
    }

    @Override
    public String buildRequestBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        if (requestDto.getStatus() == RESPONSE_IN_PROGRESS || requestDto.getStatus() == ERROR || requestDto.getStatus() == TIMEOUT) {
            final boolean hasData = requestDto.getReponseData() != null;
            final boolean hasError = controlDto.getError() != null;

            return getSerializeUtils().mapObjectToXmlString(MessageBodyDto.builder()
                    .requestUuid(controlDto.getRequestUuid())
                    .eFTIData(hasData ? new String(requestDto.getReponseData(), StandardCharsets.UTF_8) : null)
                    .status(getStatus(requestDto, hasError))
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

    private String getStatus(final RabbitRequestDto requestDto, final boolean hasError) {
        if (hasError) {
            return StatusEnum.ERROR.name();
        } else if (TIMEOUT.equals(requestDto.getStatus())) {
            return StatusEnum.TIMEOUT.name();
        }
        return StatusEnum.COMPLETE.name();
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
        final ControlEntity requestEntityControl = requestEntity.getControl();
        if (TIMEOUT.name().equalsIgnoreCase(messageBody.getStatus())) {
            requestEntity.setStatus(TIMEOUT);
            final StatusEnum controlStatus = getControlService().getControlNextStatus(requestEntityControl);
            requestEntityControl.setStatus(controlStatus);
        } else if (!ObjectUtils.isEmpty(messageBody.getEFTIData())) {
            requestEntity.setReponseData(messageBody.getEFTIData().toString().getBytes(StandardCharsets.UTF_8));
            requestEntity.setStatus(RequestStatusEnum.SUCCESS);
        } else {
            requestEntity.setStatus(ERROR);
            requestEntity.setError(setErrorFromMessageBodyDto(messageBody));
            requestEntityControl.setError(setErrorFromMessageBodyDto(messageBody));
            requestEntityControl.setStatus(StatusEnum.ERROR);
        }
        uilRequestRepository.save(requestEntity);
        return getControlService().save(requestEntityControl);
    }

    private ErrorEntity setErrorFromMessageBodyDto(final MessageBodyDto messageBody) {
        return StringUtils.isBlank(messageBody.getErrorDescription()) ?
                getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND))
                :
                getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromAnyError(messageBody.getErrorDescription()));
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

    private void responseToOtherGateIfNecessary(final UilRequestEntity uilRequestEntity) {
        if (!uilRequestEntity.getControl().isExternalAsk()) return;
        this.updateStatus(uilRequestEntity, RESPONSE_IN_PROGRESS);
        uilRequestEntity.setGateUrlDest(uilRequestEntity.getControl().getFromGateUrl());
        final UilRequestEntity savedUilRequestEntity = uilRequestRepository.save(uilRequestEntity);
        final UilRequestDto requestDto = getMapperUtils().requestToRequestDto(savedUilRequestEntity, UilRequestDto.class);
        requestDto.setRequestType(RequestType.UIL);
        this.sendRequest(requestDto);
    }

    private UilRequestEntity findByRequestUuidOrThrow(final String requestId) {
        return Optional.ofNullable(
                        this.uilRequestRepository.findByControlRequestUuidAndStatus(requestId, RequestStatusEnum.IN_PROGRESS))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find request for requestUuid: " + requestId));
    }
}
