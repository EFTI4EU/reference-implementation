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
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
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
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.UIL;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import static eu.efti.commons.enums.StatusEnum.COMPLETE;

@Slf4j
@Component
public class UilRequestService extends RequestService<UilRequestEntity> {

    private static final String UIL = "UIL";
    private final UilRequestRepository uilRequestRepository;
    private final SerializeUtils serializeUtils;

    public UilRequestService(final UilRequestRepository uilRequestRepository, final MapperUtils mapperUtils,
                             final RabbitSenderService rabbitSenderService,
                             final ControlService controlService,
                             final GateProperties gateProperties,
                             final RequestUpdaterService requestUpdaterService,
                             final SerializeUtils serializeUtils,
                             final LogManager logManager) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.uilRequestRepository = uilRequestRepository;
        this.serializeUtils = serializeUtils;
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
        final UILResponse uilResponse =
                getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), UILResponse.class);

        final UilRequestEntity uilRequestEntity = this.findByRequestUuidOrThrow(uilResponse.getRequestId());
        if (uilResponse.getStatus().equals(COMPLETE.name())) {
            uilRequestEntity.setReponseData(serializeUtils.mapObjectToXmlString(uilResponse.getConsignment()).getBytes(Charset.defaultCharset()));
            this.updateStatus(uilRequestEntity, RequestStatusEnum.SUCCESS, notificationDto.getMessageId());
        } else {
            this.updateStatus(uilRequestEntity, ERROR, notificationDto.getMessageId());
            errorReceived(uilRequestEntity, uilResponse.getDescription());
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
            externalRequest.getControl().setStatus(COMPLETE);
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
        ControlDto controlDto;
        //temporary , should be updated after the edelivery action rework
        if(notificationDto.getContent().getBody().trim().startsWith("<UILQuery")) {
            final UILQuery uilQuery = getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), UILQuery.class);
            controlDto = this.getControlService().createUilControl(ControlUtils
                    .fromGateToGateQuery(uilQuery, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH,
                            notificationDto, getGateProperties().getOwner()));
        } else {
            final UILResponse uilResponse = getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), UILResponse.class);
            final UilRequestEntity requestEntity = uilRequestRepository
                    .findByControlRequestUuidAndStatus(uilResponse.getRequestId(), RequestStatusEnum.IN_PROGRESS);
            controlDto = manageResponseFromOtherGate(requestEntity, uilResponse);
        }

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
            final boolean hasError = requestDto.getError() != null;

            final UILResponse uilResponse = new UILResponse();
            uilResponse.setRequestId(controlDto.getRequestUuid());
            uilResponse.setStatus(getStatus(requestDto, hasError));
            uilResponse.setConsignment(hasData ? serializeUtils.mapXmlStringToClass(new String(requestDto.getReponseData()), SupplyChainConsignment.class) : null);
            uilResponse.setDescription(hasError ? requestDto.getError().getErrorDescription() : null);

            return getSerializeUtils().mapObjectToXmlString(uilResponse);
        }

        final UILQuery uilQuery = new UILQuery();
        final UIL uil = new UIL();
        uil.setDatasetId(controlDto.getEftiDataUuid());
        uil.setPlatformId(requestDto.getControl().getEftiPlatformUrl());
        uil.setGateId(requestDto.getGateUrlDest());
        uilQuery.setUil(uil);
        uilQuery.setRequestId(requestDto.getControl().getRequestUuid());
        return getSerializeUtils().mapObjectToXmlString(uilQuery);
    }

    private String getStatus(final RabbitRequestDto requestDto, final boolean hasError) {
        if (hasError) {
            return StatusEnum.ERROR.name();
        } else if (TIMEOUT.equals(requestDto.getStatus())) {
            return StatusEnum.TIMEOUT.name();
        }
        return COMPLETE.name();
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


    private ControlDto manageResponseFromOtherGate(final UilRequestEntity requestEntity, final UILResponse uilResponse) {
        final ControlEntity requestEntityControl = requestEntity.getControl();
        final Optional<StatusEnum> responseStatus =  StatusEnum.fromString(uilResponse.getStatus());
        if(responseStatus.isEmpty()) {
            throw new TechnicalException("status " + uilResponse.getStatus() + " not found");
        }
        switch (responseStatus.get()) {
            case TIMEOUT -> {
                requestEntity.setStatus(TIMEOUT);
                final StatusEnum controlStatus = getControlService().getControlNextStatus(requestEntityControl);
                requestEntityControl.setStatus(controlStatus);
            }
            case COMPLETE -> {
                requestEntity.setReponseData(serializeUtils.mapObjectToXmlString(uilResponse.getConsignment()).getBytes(StandardCharsets.UTF_8));
                requestEntity.setStatus(RequestStatusEnum.SUCCESS);
            }
            case ERROR -> {
                requestEntity.setStatus(ERROR);
                requestEntity.setError(setErrorFromMessageBodyDto(uilResponse));
                requestEntityControl.setError(setErrorFromMessageBodyDto(uilResponse));
                requestEntityControl.setStatus(StatusEnum.ERROR);
            }
            default -> throw new TechnicalException("status " + uilResponse.getStatus() + " not found");


        }
        uilRequestRepository.save(requestEntity);
        return getControlService().save(requestEntityControl);
    }

    private ErrorEntity setErrorFromMessageBodyDto(final UILResponse uilResponse) {
        return StringUtils.isBlank(uilResponse.getDescription()) ?
                getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND))
                :
                getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromAnyError(uilResponse.getDescription()));
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
