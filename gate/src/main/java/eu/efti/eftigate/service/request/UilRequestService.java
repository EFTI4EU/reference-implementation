package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.UilRequestDto;
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
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public void manageQueryReceived(final NotificationDto notificationDto) {
        final UILQuery uilQuery = getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody());
        final ControlDto controlDto = this.getControlService().createUilControl(ControlUtils
                .fromGateToGateQuery(uilQuery, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH,
                        notificationDto, getGateProperties().getOwner()));

        getLogManager().logReceivedMessage(controlDto, notificationDto.getContent().getBody(), notificationDto.getContent().getFromPartyId(), LogManager.FTI_022_FTI_010);
    }

    public void manageResponseReceived(final NotificationDto notificationDto) {
        final UILResponse uilResponse = getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody());
        final UilRequestDto uilRequestDto = this.findByRequestUuidOrThrow(uilResponse.getRequestId());
        ControlDto controlDto;
        if(List.of(RequestTypeEnum.LOCAL_UIL_SEARCH, EXTERNAL_ASK_UIL_SEARCH).contains(uilRequestDto.getControl().getRequestType())) { //platform response
            controlDto = manageResponseFromPlatform(uilRequestDto, uilResponse, notificationDto.getMessageId());
        } else { // gate response
            controlDto = manageResponseFromOtherGate(uilRequestDto, uilResponse);
        }
        //log efti022
        getLogManager().logReceivedMessage(controlDto, notificationDto.getContent().getBody(), notificationDto.getContent().getFromPartyId(), LogManager.FTI_022_FTI_010);
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
    public boolean supports(final String requestType) {
        return UIL.equalsIgnoreCase(requestType);
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
            final JAXBElement<UILResponse> jaxBResponse = getObjectFactory().createUilResponse(uilResponse);
            return getSerializeUtils().mapJaxbObjectToXmlString(jaxBResponse, UILResponse.class);
        }

        final UILQuery uilQuery = new UILQuery();
        final UIL uil = new UIL();
        uil.setDatasetId(controlDto.getEftiDataUuid());
        uil.setPlatformId(requestDto.getControl().getEftiPlatformUrl());
        uil.setGateId(requestDto.getGateUrlDest());
        uilQuery.setUil(uil);
        uilQuery.setRequestId(requestDto.getControl().getRequestUuid());

        final JAXBElement<UILQuery> jaxBResponse = getObjectFactory().createUilQuery(uilQuery);
        return getSerializeUtils().mapJaxbObjectToXmlString(jaxBResponse, UILQuery.class);
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

    private ControlDto manageResponseFromPlatform(final UilRequestDto uilRequestDto, final UILResponse uilResponse, final String messageId) {
        if (uilResponse.getStatus().equals(COMPLETE.name())) {
            uilRequestDto.setReponseData(serializeUtils.mapObjectToXmlString(uilResponse.getConsignment()).getBytes(Charset.defaultCharset()));
            this.updateStatus(uilRequestDto, RequestStatusEnum.SUCCESS, messageId);
        } else {
            this.updateStatus(uilRequestDto, ERROR, messageId);
            errorReceived(uilRequestDto, uilResponse.getDescription());
        }
        return responseToOtherGateIfNecessary(uilRequestDto);
    }

    private ControlDto manageResponseFromOtherGate(final UilRequestDto requestDto, final UILResponse uilResponse) {
        final ControlDto controlDto = requestDto.getControl();
        final Optional<StatusEnum> responseStatus =  StatusEnum.fromString(uilResponse.getStatus());
        if(responseStatus.isEmpty()) {
            throw new TechnicalException("status " + uilResponse.getStatus() + " not found");
        }
        switch (responseStatus.get()) {
            case TIMEOUT -> {
                requestDto.setStatus(TIMEOUT);
                //todo avoid mapping
                final StatusEnum controlStatus = getControlService().getControlNextStatus(getMapperUtils().controlDtoToControlEntity(controlDto));
                controlDto.setStatus(controlStatus);
            }
            case COMPLETE -> {
                requestDto.setReponseData(serializeUtils.mapObjectToXmlString(uilResponse.getConsignment()).getBytes(StandardCharsets.UTF_8));
                requestDto.setStatus(RequestStatusEnum.SUCCESS);
            }
            case ERROR -> {
                requestDto.setStatus(ERROR);
                requestDto.setError(setErrorFromResponse(uilResponse));
                controlDto.setError(setErrorFromResponse(uilResponse));
                controlDto.setStatus(StatusEnum.ERROR);
            }
            default -> throw new TechnicalException("status " + uilResponse.getStatus() + " not found");


        }
        this.save(requestDto);
        return getControlService().save(controlDto);
    }

    private ErrorDto setErrorFromResponse(final UILResponse uilResponse) {
        return StringUtils.isBlank(uilResponse.getDescription()) ?
                ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND) : ErrorDto.fromAnyError(uilResponse.getDescription());
    }

    public void updateStatus(final UilRequestDto uilRequestDto, final RequestStatusEnum status, final String eDeliveryMessageId) {
        this.updateStatus(uilRequestDto, status);
        markMessageAsDownloaded(eDeliveryMessageId);
    }

    protected void errorReceived(final UilRequestDto requestDto, final String errorDescription) {
        log.error("Error received, change status of requestId : {}", requestDto.getControl().getRequestUuid());
        final ErrorDto errorDto = ErrorDto.builder()
                .errorDescription(errorDescription)
                .errorCode(ErrorCodesEnum.PLATFORM_ERROR.toString())
                .build();

        final ControlDto controlDto = requestDto.getControl();
        controlDto.setError(errorDto);
        controlDto.setStatus(StatusEnum.ERROR);

        requestDto.setControl(controlDto);
        save(requestDto);
        getControlService().save(controlDto);
    }

    private ControlDto responseToOtherGateIfNecessary(final UilRequestDto uilRequestDto) {
        if (!uilRequestDto.getControl().isExternalAsk()) {
            return uilRequestDto.getControl();
        }
        this.updateStatus(uilRequestDto, RESPONSE_IN_PROGRESS);
        uilRequestDto.setGateUrlDest(uilRequestDto.getControl().getFromGateUrl());
        final RequestDto savedUilRequestDto = this.save(uilRequestDto);
        savedUilRequestDto.setRequestType(RequestType.UIL);
        this.sendRequest(savedUilRequestDto);
        return uilRequestDto.getControl();
    }

    private UilRequestDto findByRequestUuidOrThrow(final String requestId) {
        final UilRequestEntity entity = Optional.ofNullable(
                        this.uilRequestRepository.findByControlRequestUuidAndStatus(requestId, RequestStatusEnum.IN_PROGRESS))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find request for requestUuid: " + requestId));
        return getMapperUtils().requestToRequestDto(entity, UilRequestDto.class);
    }
}
