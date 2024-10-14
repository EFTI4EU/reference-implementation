package eu.efti.eftigate.service.request;


import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.IDENTIFIERS_ACTIONS;
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

    @Override
    public void manageMessageReceive(final NotificationDto notificationDto) {
        final String bodyFromNotification = notificationDto.getContent().getBody();
        //temporary , should be updated after the edelivery action rework
        if(bodyFromNotification.trim().startsWith("<IdentifierQuery")) {
            handleNewControlRequest(notificationDto, bodyFromNotification);
        } else {
            final IdentifierResponse response = getSerializeUtils().mapXmlStringToClass(bodyFromNotification, IdentifierResponse.class);
            if (getControlService().existsByCriteria(response.getRequestId())) {
                identifiersControlUpdateDelegateService.updateExistingControl(response, notificationDto.getContent().getFromPartyId());
                identifiersControlUpdateDelegateService.setControlNextStatus(response.getRequestId());
            }
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
    public boolean supports(final EDeliveryAction eDeliveryAction) {
        return IDENTIFIERS_ACTIONS.contains(eDeliveryAction);
    }

    @Override
    public boolean supports(final String requestType) {
        return IDENTIFIER.equalsIgnoreCase(requestType);
    }

    @Override
    public void receiveGateRequest(final NotificationDto notificationDto) {
        throw new UnsupportedOperationException("Forward Operations not supported for Consignment");
    }

    @Override
    public IdentifiersRequestDto createRequest(final ControlDto controlDto) {
        return new IdentifiersRequestDto(controlDto);
    }

    @Override
    public String buildRequestBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        if (EXTERNAL_ASK_IDENTIFIERS_SEARCH == controlDto.getRequestType()) { //remote sending response
            final IdentifierResponse identifierResponse = buildEdeliveryIdentifiersResponse(requestDto);
            return getSerializeUtils().mapObjectToXmlString(identifierResponse);
        } else { //local sending request
            return getSerializeUtils().mapObjectToXmlString(this.buildQueryFromControl(controlDto));
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

    private void handleNewControlRequest(final NotificationDto notificationDto, final String bodyFromNotification) {
        final IdentifierQuery identifierQuery = getSerializeUtils().mapXmlStringToClass(bodyFromNotification, IdentifierQuery.class);
        final List<ConsignmentDto> identifiersDtoList = identifiersService.search(buildIdentifiersRequestDtoFrom(identifierQuery));
        final IdentifiersResultsDto identifiersResults = IdentifiersResultsDto.builder().consignments(identifiersDtoList).build();
        final ControlDto controlDto = getControlService().createControlFrom(identifierQuery, notificationDto.getContent().getFromPartyId(), identifiersResults);
        final RequestDto request = createReceivedRequest(controlDto, identifiersDtoList);
        final RequestDto updatedRequest = this.updateStatus(request, RESPONSE_IN_PROGRESS);
        super.sendRequest(updatedRequest);
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
                .vehicleID(identifierQuery.getIdentifier().getValue())
                .isDangerousGoods(identifierQuery.isDangerousGoodsIndicator())
                .transportMode(identifierQuery.getModeCode())
                .vehicleCountry(identifierQuery.getRegistrationCountryCode())
                .build();
    }

    private IdentifierQuery buildQueryFromControl(final ControlDto controlDto) {
        final SearchParameter searchParameter = controlDto.getTransportIdentifiers();
        final IdentifierQuery identifierQuery = new IdentifierQuery();
        identifierQuery.setRequestId(controlDto.getRequestUuid());
        if (searchParameter != null){
            final Identifier identifier = new Identifier();
            identifier.setValue(searchParameter.getVehicleID());
            identifierQuery.setIdentifier(identifier);
            identifierQuery.setModeCode(searchParameter.getTransportMode());
            identifierQuery.setDangerousGoodsIndicator(searchParameter.getIsDangerousGoods());
            identifierQuery.setRegistrationCountryCode(searchParameter.getVehicleCountry());
        }
        return identifierQuery;
    }

    private IdentifierResponse buildEdeliveryIdentifiersResponse(RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        final IdentifierResponse identifierResponse = new IdentifierResponse();
        identifierResponse.setRequestId(controlDto.getRequestUuid());
        identifierResponse.setStatus(controlDto.getStatus().name());
        if(controlDto.getError() != null) {
            identifierResponse.setDescription(controlDto.getError().getErrorDescription());
        }
        identifierResponse.getConsignment().addAll(getMapperUtils().dtoToEdelivery(requestDto.getIdentifiersResults().getConsignments()));
        return identifierResponse;
    }
}
