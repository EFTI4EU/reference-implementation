package eu.efti.eftigate.service.request;


import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.constant.EDeliveryStatus;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.edelivery.Identifier;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierResponse;
import eu.efti.v1.edelivery.IdentifierType;
import eu.efti.v1.edelivery.ObjectFactory;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.IDENTIFIERS_TYPES;
import static eu.efti.commons.enums.RequestStatusEnum.RECEIVED;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Component
public class IdentifiersRequestService extends RequestService<IdentifiersRequestEntity> {

    public static final String IDENTIFIER = "IDENTIFIER";
    @Lazy
    private final IdentifiersService identifiersService;
    private final IdentifiersRequestRepository identifiersRequestRepository;
    private final IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;

    public IdentifiersRequestService(final IdentifiersRequestRepository identifiersRequestRepository,
                                     final MapperUtils mapperUtils,
                                     final RabbitSenderService rabbitSenderService,
                                     final ControlService controlService,
                                     final GateProperties gateProperties,
                                     final IdentifiersService identifiersService,
                                     final RequestUpdaterService requestUpdaterService,
                                     final SerializeUtils serializeUtils,
                                     final LogManager logManager,
                                     final IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.identifiersService = identifiersService;
        this.identifiersRequestRepository = identifiersRequestRepository;
        this.identifiersControlUpdateDelegateService = identifiersControlUpdateDelegateService;
    }


    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getIdentifiersResults()) && isNotEmpty(requestEntity.getIdentifiersResults().getConsignments()));
    }

    public void manageQueryReceived(final NotificationDto notificationDto) {
        final IdentifierQuery identifierQuery = getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody());
        final List<ConsignmentDto> identifiersDtoList = identifiersService.search(buildIdentifiersRequestDtoFrom(identifierQuery));
        final IdentifiersResultsDto identifiersResults = IdentifiersResultsDto.builder().consignments(identifiersDtoList).build();
        final ControlDto controlDto = getControlService().createControlFrom(identifierQuery, notificationDto.getContent().getFromPartyId(), identifiersResults);
        final RequestDto request = createReceivedRequest(controlDto, identifiersDtoList);
        final RequestDto updatedRequest = this.updateStatus(request, RESPONSE_IN_PROGRESS);
        super.sendRequest(updatedRequest);
    }

    public void manageResponseReceived(final NotificationDto notificationDto) {
        final IdentifierResponse response = getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody());
        if (getControlService().existsByCriteria(response.getRequestId())) {
            identifiersControlUpdateDelegateService.updateExistingControl(response, notificationDto.getContent().getFromPartyId());
            identifiersControlUpdateDelegateService.setControlNextStatus(response.getRequestId());
        }
    }

    @Override
    public void manageSendSuccess(final String eDeliveryMessageId) {
        final IdentifiersRequestEntity externalRequest = identifiersRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(EXTERNAL_ASK_IDENTIFIERS_SEARCH,
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

    @Override
    public void updateSentRequestStatus(final RequestDto requestDto, final String edeliveryMessageId) {
        requestDto.setEdeliveryMessageId(edeliveryMessageId);
        this.updateStatus(requestDto, isExternalRequest(requestDto) ? RESPONSE_IN_PROGRESS : RequestStatusEnum.IN_PROGRESS);
    }

    public void createOrUpdate(final NotificationDto notificationDto) {
        this.identifiersService.createOrUpdate(new SaveIdentifiersRequestWrapper(notificationDto.getContent().getFromPartyId(),
                getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody())));
    }

    private RequestDto createReceivedRequest(final ControlDto controlDto, final List<ConsignmentDto> identifiersDtos) {
        final RequestDto request = createRequest(controlDto, RECEIVED, identifiersDtos);
        final ControlDto updatedControl = getControlService().getControlByRequestUuid(controlDto.getRequestUuid());
        if (StatusEnum.COMPLETE == updatedControl.getStatus()) {
            request.setStatus(RESPONSE_IN_PROGRESS);
        }
        request.setControl(updatedControl);
        return request;
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
                .gateUrlDest(controlDto.getFromGateUrl())
                .requestType(RequestType.IDENTIFIER)
                .build();
    }

    private SearchWithIdentifiersRequestDto buildIdentifiersRequestDtoFrom(final IdentifierQuery identifierQuery) {
        return SearchWithIdentifiersRequestDto.builder()
                .identifier(identifierQuery.getIdentifier().getValue())
                .dangerousGoodsIndicator(identifierQuery.isDangerousGoodsIndicator())
                .modeCode(identifierQuery.getModeCode())
                .registrationCountryCode(identifierQuery.getRegistrationCountryCode())
                .build();
    }

    private JAXBElement<IdentifierQuery> buildQueryFromControl(final ControlDto controlDto) {
        final SearchParameter searchParameter = controlDto.getTransportIdentifiers();
        final IdentifierQuery identifierQuery = new IdentifierQuery();
        identifierQuery.setRequestId(controlDto.getRequestUuid());
        if (searchParameter != null) {
            final Identifier identifier = new Identifier();
            identifier.setValue(searchParameter.getIdentifier());
            try {
                CollectionUtils.emptyIfNull(searchParameter.getIdentifierType()).forEach(type -> identifier.getType().add(IdentifierType.fromValue(type)));
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
        identifierResponse.setRequestId(controlDto.getRequestUuid());
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
