package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.constant.EDeliveryStatus;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.ErrorEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.IdentifiersControlUpdateDelegateService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.eftigate.service.ValidationService;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftilogger.model.RequestTypeLog;
import eu.efti.eftilogger.service.ReportingRequestLogService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.edelivery.Identifier;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierResponse;
import eu.efti.v1.edelivery.IdentifierType;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.IDENTIFIERS_TYPES;
import static eu.efti.commons.enums.ErrorCodesEnum.XML_ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.RECEIVED;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.REGISTRY;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Component
public class IdentifiersRequestService extends RequestService<IdentifiersRequestEntity> {

    public static final String IDENTIFIER = "IDENTIFIER";
    @Lazy
    private final IdentifiersService identifiersService;
    private final IdentifiersRequestRepository identifiersRequestRepository;
    private final IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;
    private final ValidationService validationService;
    private final GateProperties gateProperties;

    private final ReportingRequestLogService reportingRequestLogService;

    private final EftiGateIdResolver eftiGateIdResolver;

    public IdentifiersRequestService(final IdentifiersRequestRepository identifiersRequestRepository,
                                     final MapperUtils mapperUtils,
                                     final RabbitSenderService rabbitSenderService,
                                     final ControlService controlService,
                                     final GateProperties gateProperties,
                                     final IdentifiersService identifiersService,
                                     final RequestUpdaterService requestUpdaterService,
                                     final SerializeUtils serializeUtils,
                                     final LogManager logManager,
                                     final IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService,
                                     final ValidationService validationService,
                                     final ReportingRequestLogService reportingRequestLogService,
                                     final EftiGateIdResolver eftiGateIdResolver) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.identifiersService = identifiersService;
        this.identifiersRequestRepository = identifiersRequestRepository;
        this.identifiersControlUpdateDelegateService = identifiersControlUpdateDelegateService;
        this.validationService = validationService;
        this.gateProperties = gateProperties;
        this.reportingRequestLogService = reportingRequestLogService;
        this.eftiGateIdResolver = eftiGateIdResolver;
    }

    @Override
    public Optional<RequestDto> findRequestDtoByRequestType(ControlDto controlDto) {
        throw new UnsupportedOperationException("Method not supported");
    }

    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getIdentifiersResults()) && isNotEmpty(requestEntity.getIdentifiersResults().getConsignments()));
    }

    public void manageQueryReceived(final NotificationDto notificationDto) {
        NotificationContentDto content = notificationDto.getContent();
        String body = content.getBody();
        String fromPartyId = notificationDto.getContent().getFromPartyId();
        try {
            validationService.validateXml(body);
            final IdentifierQuery identifierQuery = getSerializeUtils().mapXmlStringToJaxbObject(body);
            final ControlDto controlDto = getControlService().createControlFrom(identifierQuery, fromPartyId);
            //log fti019
            getLogManager().logReceivedMessage(controlDto, GATE, GATE, body, fromPartyId, StatusEnum.COMPLETE, LogManager.FTI_019);
            final List<ConsignmentDto> identifiersDtoList = identifiersService.search(buildIdentifiersRequestDtoFrom(identifierQuery));
            //log fti015
            getLogManager().logRequestRegistry(controlDto, null, GATE, REGISTRY, LogManager.FTI_015);
            controlDto.setIdentifiersResults(identifiersDtoList);
            getControlService().save(controlDto);
            //log fti016
            getLogManager().logRequestRegistry(controlDto, getSerializeUtils().mapObjectToBase64String(identifiersDtoList), REGISTRY, GATE, LogManager.FTI_016);
            final RequestDto request = createReceivedRequest(controlDto, identifiersDtoList);
            final RequestDto updatedRequest = this.updateStatus(request, RESPONSE_IN_PROGRESS);
            super.sendRequest(updatedRequest);
        } catch (SAXException e) {
            log.error("Received invalid IdentifierQuery from {}, {}", content.getFromPartyId(), e.getMessage());
            RequestDto errorRequestDto = this.buildErrorRequestDto(notificationDto, EXTERNAL_ASK_IDENTIFIERS_SEARCH, e.getMessage(), XML_ERROR.name(), RequestType.IDENTIFIER);
            this.sendRequest(errorRequestDto);
            getLogManager().logReceivedMessage(errorRequestDto.getControl(), GATE, GATE, body, fromPartyId, StatusEnum.ERROR, LogManager.FTI_019);

        } catch (IllegalArgumentException | IOException e) {
            log.error("Something went wrong when validating IdentifierQuery from{}", content.getFromPartyId(), e);
            RequestDto errorRequestDto = this.buildErrorRequestDto(notificationDto, EXTERNAL_ASK_IDENTIFIERS_SEARCH, e.getMessage(), XML_ERROR.name(), RequestType.IDENTIFIER);
            this.sendRequest(errorRequestDto);
            getLogManager().logReceivedMessage(errorRequestDto.getControl(), GATE, GATE, body, fromPartyId, StatusEnum.ERROR, LogManager.FTI_019);
        }
    }

    public void manageResponseReceived(final NotificationDto notificationDto) {
        NotificationContentDto content = notificationDto.getContent();
        String body = content.getBody();
        try {
            validationService.validateXml(body);
            final IdentifierResponse response = getSerializeUtils().mapXmlStringToJaxbObject(body);
            String requestId = response.getRequestId();
            if (getControlService().findByRequestId(requestId).isPresent()) {
                String fromPartyId = content.getFromPartyId();
                identifiersControlUpdateDelegateService.updateExistingControl(response, fromPartyId);
                identifiersControlUpdateDelegateService.setControlNextStatus(requestId);
                IdentifiersRequestEntity identifiersRequestEntity = identifiersRequestRepository.findByControlRequestIdAndGateIdDest(requestId, fromPartyId);
                ControlDto controlDto = getMapperUtils().controlEntityToControlDto(identifiersRequestEntity.getControl());
                RequestDto requestDto = getMapperUtils().identifiersRequestEntityToRequestDto(identifiersRequestEntity, RequestDto.class);
                String currentGateCountry = getGateProperties().getCountry();
                String owner = getGateProperties().getOwner();
                reportingRequestLogService.logReportingRequest(controlDto, requestDto, owner, currentGateCountry, RequestTypeLog.IDENTIFIERS, GATE, controlDto.getFromGateId(), eftiGateIdResolver.resolve(controlDto.getFromGateId()), GATE, owner, currentGateCountry, true);
                //log fti021
                getLogManager().logReceivedMessage(controlDto, GATE, GATE, body, fromPartyId,
                        getStatusEnumOfRequest(identifiersRequestEntity), LogManager.FTI_021);
            }
        } catch (SAXException e) {
            String exceptionMessage = e.getMessage();
            log.error("Received invalid IdentifierResponse from {}, {}", content.getFromPartyId(), exceptionMessage);
            String conversationId = notificationDto.getContent().getConversationId();
            IdentifiersRequestEntity request = identifiersRequestRepository.findByControlRequestIdAndGateIdDest(conversationId, notificationDto.getContent().getFromPartyId());
            if (request != null) {
                this.updateRequestWithError(exceptionMessage, request);
            }
        } catch (IllegalArgumentException | IOException e) {
            log.error("Something went wrong when validating IdentifierResponse from{}", content.getFromPartyId(), e);
            this.sendRequest(this.buildErrorRequestDto(notificationDto, EXTERNAL_ASK_IDENTIFIERS_SEARCH, e.getMessage(), XML_ERROR.name(), RequestType.IDENTIFIER));
        }
    }

    @Override
    public void manageSendSuccess(final String eDeliveryMessageId) {
        final IdentifiersRequestEntity externalRequest = identifiersRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(EXTERNAL_ASK_IDENTIFIERS_SEARCH,
                RESPONSE_IN_PROGRESS, eDeliveryMessageId);
        if (externalRequest == null) {
            log.info("sent message {} successfully", eDeliveryMessageId);
        } else {
            externalRequest.getControl().setStatus(StatusEnum.COMPLETE);
            this.updateStatus(externalRequest, SUCCESS);
        }
    }

    @Override
    public boolean supports(final RequestTypeEnum requestTypeEnum) {
        return IDENTIFIERS_TYPES.contains(requestTypeEnum);
    }

    @Override
    public boolean supports(final String requestType) {
        return IDENTIFIER.equalsIgnoreCase(requestType);
    }

    @Override
    public IdentifiersRequestDto createRequest(final ControlDto controlDto) {
        return new IdentifiersRequestDto(controlDto);
    }

    @Override
    public String buildRequestBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        if (EXTERNAL_ASK_IDENTIFIERS_SEARCH == controlDto.getRequestType()) { //remote sending response
            return getSerializeUtils().mapJaxbObjectToXmlString(this.buildEdeliveryIdentifiersResponse(requestDto), IdentifierResponse.class);
        } else { //local sending request
            return getSerializeUtils().mapJaxbObjectToXmlString(this.buildQueryFromControl(controlDto), IdentifierQuery.class);
        }
    }

    @Override
    public IdentifiersRequestDto save(final RequestDto requestDto) {
        return getMapperUtils().requestToRequestDto(
                identifiersRequestRepository.save(getMapperUtils().requestDtoToRequestEntity(requestDto, IdentifiersRequestEntity.class)),
                IdentifiersRequestDto.class);
    }

    @Override
    public void saveRequest(final RequestDto requestDto) {
        identifiersRequestRepository.save(getMapperUtils().requestDtoToRequestEntity(requestDto, IdentifiersRequestEntity.class));
    }

    @Override
    protected void updateStatus(final IdentifiersRequestEntity identifiersRequestEntity, final RequestStatusEnum status) {
        identifiersRequestEntity.setStatus(status);
        getControlService().save(identifiersRequestEntity.getControl());
        identifiersRequestRepository.save(identifiersRequestEntity);
    }

    @Override
    protected IdentifiersRequestEntity findRequestByMessageIdOrThrow(final String eDeliveryMessageId) {
        return Optional.ofNullable(this.identifiersRequestRepository.findByEdeliveryMessageId(eDeliveryMessageId))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find Consignment request for messageId: " + eDeliveryMessageId));
    }

    public void updateRequestWithError(String exceptionMessage, IdentifiersRequestEntity request) {
        ErrorEntity errorEntity = ErrorEntity.builder()
                .errorCode(ErrorCodesEnum.XML_ERROR.name())
                .errorDescription(exceptionMessage).build();
        request.setError(errorEntity);
        ControlEntity control = request.getControl();
        if (control.isExternalAsk()) {
            this.sendRequest(getMapperUtils().requestToRequestDto(request, IdentifiersRequestDto.class));
        } else {
            control.setStatus(StatusEnum.ERROR);
            control.setError(errorEntity);
            this.updateStatus(request, ERROR);
        }
    }

    @Override
    public void updateRequestStatus(final RequestDto requestDto, final String edeliveryMessageId) {
        requestDto.setEdeliveryMessageId(edeliveryMessageId);
        this.updateStatus(requestDto, isExternalRequest(requestDto) ? RESPONSE_IN_PROGRESS : RequestStatusEnum.IN_PROGRESS);
    }

    @Override
    public List<IdentifiersRequestEntity> findAllForControlId(final int controlId) {
        return identifiersRequestRepository.findByControlId(controlId);
    }

    public void createOrUpdate(final NotificationDto notificationDto) {
        try {
            validationService.validateXml(notificationDto.getContent().getBody());
            this.identifiersService.createOrUpdate(new SaveIdentifiersRequestWrapper(notificationDto.getContent().getFromPartyId(),
                    getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody())));
        } catch (SAXException e) {
            log.error("Received invalid SaveIdentifierRequest from {}, {}", notificationDto.getContent().getFromPartyId(), e.getMessage());
        } catch (IllegalArgumentException | IOException e) {
            log.error("Something went wrong when validating SaveIdentifierRequest from{}", notificationDto.getContent().getFromPartyId(), e);
        }
    }

    private RequestDto createReceivedRequest(final ControlDto controlDto, final List<ConsignmentDto> identifiersDtos) {
        final RequestDto request = createRequest(controlDto, RECEIVED, identifiersDtos);
        final ControlDto updatedControl = updateControl(controlDto);
        if (StatusEnum.COMPLETE == updatedControl.getStatus()) {
            request.setStatus(RESPONSE_IN_PROGRESS);
        }
        request.setControl(updatedControl);
        return request;
    }

    public ControlDto updateControl(final ControlDto controlDto) {
        return getControlService().updateControl(controlDto.getRequestId());
    }

    public IdentifiersRequestDto createRequest(final ControlDto controlDto, final RequestStatusEnum status, final List<ConsignmentDto> identifiersDtoList) {
        final IdentifiersRequestDto requestDto = save(buildRequestDto(controlDto, status, identifiersDtoList));
        log.info("Request has been register with controlId : {}", requestDto.getControl().getId());
        return requestDto;
    }

    private IdentifiersRequestDto buildRequestDto(final ControlDto controlDto, final RequestStatusEnum status, final List<ConsignmentDto> identifiersDtoList) {
        return IdentifiersRequestDto.builder()
                .retry(0)
                .control(controlDto)
                .status(status)
                .identifiersResults(IdentifiersResultsDto.builder().consignments(identifiersDtoList).build())
                .gateIdDest(controlDto.getFromGateId() != null ? controlDto.getFromGateId() : gateProperties.getOwner())
                .requestType(RequestType.IDENTIFIER)
                .build();
    }

    private SearchWithIdentifiersRequestDto buildIdentifiersRequestDtoFrom(final IdentifierQuery identifierQuery) {
        Identifier identifier = identifierQuery.getIdentifier();
        return SearchWithIdentifiersRequestDto.builder()
                .identifier(identifier.getValue())
                .identifierType(CollectionUtils.emptyIfNull(identifier.getType()).stream().map(IdentifierType::value).toList())
                .dangerousGoodsIndicator(identifierQuery.isDangerousGoodsIndicator())
                .modeCode(identifierQuery.getModeCode())
                .registrationCountryCode(identifierQuery.getRegistrationCountryCode())
                .build();
    }

    private JAXBElement<IdentifierQuery> buildQueryFromControl(final ControlDto controlDto) {
        final SearchParameter searchParameter = controlDto.getTransportIdentifiers();
        final IdentifierQuery identifierQuery = new IdentifierQuery();
        identifierQuery.setRequestId(controlDto.getRequestId());
        if (searchParameter != null) {
            final Identifier identifier = new Identifier();
            identifier.setValue(searchParameter.getIdentifier());
            try {
                CollectionUtils.emptyIfNull(searchParameter.getIdentifierType()).stream()
                        .filter(StringUtils::isNotBlank)
                        .forEach(type -> identifier.getType().add(IdentifierType.fromValue(type.toLowerCase())));
            } catch (final IllegalArgumentException e) {
                log.error("unknown identifier type {}", e.getMessage());
            }
            identifierQuery.setIdentifier(identifier);
            identifierQuery.setModeCode(searchParameter.getModeCode());
            identifierQuery.setDangerousGoodsIndicator(searchParameter.getDangerousGoodsIndicator());
            identifierQuery.setRegistrationCountryCode(searchParameter.getRegistrationCountryCode());
        }

        return getObjectFactory().createIdentifierQuery(identifierQuery);
    }

    private JAXBElement<IdentifierResponse> buildEdeliveryIdentifiersResponse(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        final IdentifierResponse identifierResponse = new IdentifierResponse();
        identifierResponse.setRequestId(controlDto.getRequestId());
        identifierResponse.setStatus(EDeliveryStatus.OK.getCode());
        if (controlDto.getError() != null) {
            identifierResponse.setDescription(controlDto.getError().getErrorDescription());
        }
        if (requestDto.getIdentifiersResults() != null) {
            identifierResponse.getConsignment().addAll(getMapperUtils().dtoToEdelivery(requestDto.getIdentifiersResults().getConsignments()));
        }
        return getObjectFactory().createIdentifierResponse(identifierResponse);
    }
}
