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
import eu.efti.edeliveryapconnector.constant.EDeliveryStatus;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
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
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.v1.consignment.common.ObjectFactory;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.UIL;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.REQUEST_STATUS_ENUM_STATUS_ENUM_MAP;
import static eu.efti.commons.constant.EftiGateConstants.UIL_TYPES;
import static eu.efti.commons.enums.ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY;
import static eu.efti.commons.enums.RequestStatusEnum.ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.RequestStatusEnum.TIMEOUT;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;
import static eu.efti.commons.enums.StatusEnum.COMPLETE;
import static eu.efti.edeliveryapconnector.constant.EDeliveryStatus.isNotFound;

@Slf4j
@Component
public class UilRequestService extends RequestService<UilRequestEntity> {

    private static final String UIL = "UIL";
    private final UilRequestRepository uilRequestRepository;
    private final SerializeUtils serializeUtils;
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final ValidationService validationService;

    public UilRequestService(final UilRequestRepository uilRequestRepository, final MapperUtils mapperUtils,
                             final RabbitSenderService rabbitSenderService,
                             final ControlService controlService,
                             final GateProperties gateProperties,
                             final RequestUpdaterService requestUpdaterService,
                             final SerializeUtils serializeUtils,
                             final ValidationService validationService,
                             final LogManager logManager) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.uilRequestRepository = uilRequestRepository;
        this.serializeUtils = serializeUtils;
        this.validationService = validationService;
    }


    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(UilRequestEntity.class::isInstance)
                .map(UilRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getReponseData()));
    }

    public void manageQueryReceived(final NotificationDto notificationDto) {
        NotificationContentDto content = notificationDto.getContent();
        String body = content.getBody();
        final UILQuery uilQuery = getSerializeUtils().mapXmlStringToJaxbObject(body);
        if (!validationService.isRequestValid(uilQuery)) {
            this.sendRequest(this.buildErrorRequestDto(notificationDto, EXTERNAL_ASK_UIL_SEARCH));
            return;
        }
        getControlService().createUilControl(ControlUtils
                .fromGateToGateQuery(uilQuery, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, notificationDto, getGateProperties().getOwner()));
    }

    public void manageResponseReceived(final NotificationDto notificationDto) {
        NotificationContentDto content = notificationDto.getContent();
        String body = content.getBody();
        final UILResponse uilResponse = getSerializeUtils().mapXmlStringToJaxbObject(body);
        if (!validationService.isResponseValid(uilResponse)) {
            this.sendRequest(this.buildErrorRequestDto(notificationDto, EXTERNAL_ASK_UIL_SEARCH));
            return;
        }
        final Optional<UilRequestDto> uilRequestDto = this.findByRequestId(uilResponse.getRequestId());
        if (uilRequestDto.isPresent()) {
            if (List.of(RequestTypeEnum.LOCAL_UIL_SEARCH, EXTERNAL_ASK_UIL_SEARCH).contains(uilRequestDto.get().getControl().getRequestType())) { //platform response
                manageResponseFromPlatform(uilRequestDto.get(), uilResponse, notificationDto);
            } else { // gate response
                manageResponseFromOtherGate(uilRequestDto.get(), uilResponse, notificationDto.getContent());
            }
        } else {
            log.error("uilRequestDto not find in DB");
        }
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
            final boolean hasError = (controlDto.getError() != null || requestDto.getError() != null);

            final UILResponse uilResponse = new UILResponse();
            uilResponse.setRequestId(controlDto.getRequestId());
            uilResponse.setStatus(getStatus(requestDto, hasError));
            if (hasData) {
                try {
                    SupplyChainConsignment consignment = serializeUtils.mapXmlStringToJaxbObject(new String(requestDto.getReponseData()), JAXBContext.newInstance(ObjectFactory.class));
                    uilResponse.setConsignment(consignment);
                } catch (JAXBException e) {
                    throw new TechnicalException("error while writing content", e);
                }
            }
            uilResponse.setDescription(hasError ? requestDto.getError().getErrorDescription() : null);
            final JAXBElement<UILResponse> jaxBResponse = getObjectFactory().createUilResponse(uilResponse);
            return getSerializeUtils().mapJaxbObjectToXmlString(jaxBResponse, UILResponse.class);
        }

        final UILQuery uilQuery = new UILQuery();
        final UIL uil = new UIL();
        uil.setDatasetId(controlDto.getDatasetId());
        uil.setPlatformId(requestDto.getControl().getPlatformId());
        uil.setGateId(requestDto.getGateIdDest());
        uilQuery.setUil(uil);
        uilQuery.setRequestId(requestDto.getControl().getRequestId());
        uilQuery.getSubsetId().add(controlDto.getSubsetId());

        final JAXBElement<UILQuery> jaxBResponse = getObjectFactory().createUilQuery(uilQuery);
        return getSerializeUtils().mapJaxbObjectToXmlString(jaxBResponse, UILQuery.class);
    }

    private String getStatus(final RabbitRequestDto requestDto, final boolean hasError) {
        if (hasError) {
            String errorCode = requestDto.getError().getErrorCode();
            if (isNotFound(errorCode) || DATA_NOT_FOUND_ON_REGISTRY.name().equalsIgnoreCase(errorCode)) {
                return EDeliveryStatus.NOT_FOUND.getCode();
            }
            return EDeliveryStatus.BAD_REQUEST.getCode();
        } else if (TIMEOUT.equals(requestDto.getStatus())) {
            return EDeliveryStatus.GATEWAY_TIMEOUT.getCode();
        }
        return EDeliveryStatus.OK.getCode();
    }

    @Override
    public RequestDto save(final RequestDto requestDto) {
        return getMapperUtils().requestToRequestDto(
                uilRequestRepository.save(getMapperUtils().requestDtoToRequestEntity(requestDto, UilRequestEntity.class)),
                UilRequestDto.class);
    }

    @Override
    public void saveRequest(RequestDto requestDto) {
        uilRequestRepository.save(
                getMapperUtils().requestDtoToRequestEntity(requestDto, UilRequestEntity.class));
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

    @Override
    public List<UilRequestEntity> findAllForControlId(int controlId) {
        throw new UnsupportedOperationException("Operation not allowed for UIL Request");
    }

    private void manageResponseFromPlatform(final UilRequestDto uilRequestDto, final UILResponse uilResponse, final NotificationDto notificationDto) {
        String messageId = notificationDto.getMessageId();
        if (uilResponse.getStatus().equals(EDeliveryStatus.OK.getCode())) {
            JAXBElement<SupplyChainConsignment> consignment = objectFactory.createConsignment(uilResponse.getConsignment());
            String responseData = serializeUtils.mapJaxbObjectToXmlString(consignment, SupplyChainConsignment.class);
            uilRequestDto.setReponseData(responseData.getBytes(Charset.defaultCharset()));
            this.updateStatus(uilRequestDto, RequestStatusEnum.SUCCESS, messageId);
            getControlService().updateControlStatus(uilRequestDto.getControl(), COMPLETE);
        } else {
            this.updateStatus(uilRequestDto, ERROR, messageId);
            manageErrorReceived(uilRequestDto, uilResponse.getStatus(), uilResponse.getDescription());
        }
        if (uilRequestDto.getControl().isExternalAsk()) {
            respondToOtherGate(uilRequestDto);
        }
        //log fti010
        NotificationContentDto content = notificationDto.getContent();
        getLogManager().logReceivedMessage(uilRequestDto.getControl(), ComponentType.PLATFORM, ComponentType.GATE, content.getBody(), content.getFromPartyId(), REQUEST_STATUS_ENUM_STATUS_ENUM_MAP.getOrDefault(uilRequestDto.getStatus(), COMPLETE), LogManager.FTI_010);
    }

    private void manageResponseFromOtherGate(final UilRequestDto requestDto, final UILResponse uilResponse, NotificationContentDto content) {
        final ControlDto controlDto = requestDto.getControl();
        String uilResponseStatus = uilResponse.getStatus();
        final Optional<EDeliveryStatus> responseStatus = EDeliveryStatus.fromCode(uilResponseStatus);
        if (responseStatus.isEmpty()) {
            throw new TechnicalException("status " + uilResponseStatus + " not found");
        }
        switch (responseStatus.get()) {
            case GATEWAY_TIMEOUT -> {
                requestDto.setStatus(TIMEOUT);
                controlDto.setStatus(StatusEnum.TIMEOUT);
            }
            case OK -> {
                JAXBElement<SupplyChainConsignment> consignment = objectFactory.createConsignment(uilResponse.getConsignment());
                String responseData = serializeUtils.mapJaxbObjectToXmlString(consignment, SupplyChainConsignment.class);
                requestDto.setReponseData(responseData.getBytes(StandardCharsets.UTF_8));
                requestDto.setStatus(RequestStatusEnum.SUCCESS);
                controlDto.setStatus(COMPLETE);
            }
            case BAD_REQUEST, NOT_FOUND -> {
                requestDto.setStatus(ERROR);
                requestDto.setError(setErrorFromResponse(uilResponse));
                controlDto.setError(setErrorFromResponse(uilResponse));
                controlDto.setStatus(StatusEnum.ERROR);
            }
            default -> throw new TechnicalException("status " + uilResponseStatus + " not found");


        }
        this.save(requestDto);
        ControlDto savedControl = getControlService().save(controlDto);
        if (!StatusEnum.PENDING.equals(savedControl.getStatus())) {
            getLogManager().logReceivedMessage(controlDto, ComponentType.GATE, ComponentType.GATE, content.getBody(), content.getFromPartyId(), savedControl.getStatus(), LogManager.FTI_022);
        }
    }

    private ErrorDto setErrorFromResponse(final UILResponse uilResponse) {
        String uilResponseDescription = uilResponse.getDescription();
        if (StringUtils.isBlank(uilResponseDescription)) {
            return ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND);
        }
        return getErrorCodeFromDescription(uilResponseDescription);
    }

    private ErrorDto getErrorCodeFromDescription(String description) {
        Optional<ErrorCodesEnum> errorCode = ErrorCodesEnum.fromMessage(description);
        if (errorCode.isPresent()) {
            return ErrorDto.fromErrorCode(errorCode.get());
        } else {
            return ErrorDto.fromAnyError(description);
        }
    }

    public void updateStatus(final UilRequestDto uilRequestDto, final RequestStatusEnum status, final String eDeliveryMessageId) {
        this.updateStatus(uilRequestDto, status);
        markMessageAsDownloaded(eDeliveryMessageId);
    }

    protected void manageErrorReceived(final UilRequestDto requestDto, final String errorCode, final String errorDescription) {
        log.error("Error received, change status of requestId : {}", requestDto.getControl().getRequestId());
        final String codeString = EDeliveryStatus.fromCode(errorCode).orElse(EDeliveryStatus.BAD_REQUEST).name();
        final ErrorDto errorDto = ErrorDto.builder()
                .errorCode(codeString)
                .errorDescription(errorDescription)
                .build();

        final ControlDto controlDto = requestDto.getControl();
        controlDto.setError(errorDto);
        controlDto.setStatus(StatusEnum.ERROR);
        requestDto.setError(errorDto);
        requestDto.setControl(controlDto);
        save(requestDto);
        getControlService().save(controlDto);
    }

    private void respondToOtherGate(final UilRequestDto uilRequestDto) {
        this.updateStatus(uilRequestDto, RESPONSE_IN_PROGRESS);
        uilRequestDto.setGateIdDest(uilRequestDto.getControl().getFromGateId());
        final RequestDto savedUilRequestDto = this.save(uilRequestDto);
        savedUilRequestDto.setRequestType(RequestType.UIL);
        this.sendRequest(savedUilRequestDto);
    }

    private Optional<UilRequestDto> findByRequestId(final String requestId) {
        final Optional<UilRequestEntity> entity = Optional.ofNullable(
                this.uilRequestRepository.findByControlRequestIdAndStatus(requestId, RequestStatusEnum.IN_PROGRESS));
        return entity.map(uilRequestEntity -> getMapperUtils().requestToRequestDto(uilRequestEntity, UilRequestDto.class));
    }
}
