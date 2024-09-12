package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.*;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.IdentifiersMessageBodyDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.dto.requestbody.IdentifiersRequestBodyDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResult;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public IdentifiersRequestService(final IdentifiersRequestRepository identifiersRequestRepository,
                                     final MapperUtils mapperUtils,
                                     final RabbitSenderService rabbitSenderService,
                                     final ControlService controlService,
                                     final GateProperties gateProperties,
                                     final IdentifiersService identifiersService,
                                     final RequestUpdaterService requestUpdaterService,
                                     final SerializeUtils serializeUtils,
                                     final LogManager logManager) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.identifiersService = identifiersService;
        this.identifiersRequestRepository = identifiersRequestRepository;
    }


    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getIdentifiersResults()) && isNotEmpty(requestEntity.getIdentifiersResults().getIdentifiersResult()));
    }

    @Override
    public void setDataFromRequests(final ControlEntity controlEntity) {
        final List<IdentifiersResult> identifiersResults = controlEntity.getRequests().stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .flatMap(request -> request.getIdentifiersResults().getIdentifiersResult().stream())
                .toList();
        controlEntity.setIdentifiersResults(new IdentifiersResults(identifiersResults));
    }

    @Override
    public void manageMessageReceive(final NotificationDto notificationDto) {
        final String bodyFromNotification = notificationDto.getContent().getBody();
        if (StringUtils.isNotBlank(bodyFromNotification)){
            final String requestUuid = getRequestUuid(bodyFromNotification);
            final ControlEntity existingControl = getControlService().getControlForCriteria(requestUuid, RequestStatusEnum.IN_PROGRESS);
            if (existingControl != null) {
                updateExistingControl(bodyFromNotification, existingControl, notificationDto);
            } else {
                handleNewControlRequest(notificationDto, bodyFromNotification);
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
    protected void sendRequest(final RequestDto requestDto) {
        final RequestDto updatedRequest = this.updateStatus(requestDto, RequestStatusEnum.RESPONSE_IN_PROGRESS);
        super.sendRequest(updatedRequest);
    }

    @Override
    public void updateSentRequestStatus(final RequestDto requestDto, final String edeliveryMessageId) {
        requestDto.setEdeliveryMessageId(edeliveryMessageId);
        this.updateStatus(requestDto, isExternalRequest(requestDto) ? RequestStatusEnum.RESPONSE_IN_PROGRESS : RequestStatusEnum.IN_PROGRESS);

    }

    private void handleNewControlRequest(final NotificationDto notificationDto, final String bodyFromNotification) {
        final IdentifiersMessageBodyDto requestMessage = getSerializeUtils().mapXmlStringToClass(bodyFromNotification, IdentifiersMessageBodyDto.class);
        final List<IdentifiersDto> identifiersDtoList = identifiersService.search(buildIdentifiersRequestDtoFrom(requestMessage));
        final IdentifiersResultsDto identifiersResultsDto = buildIdentifiersResultDto(identifiersDtoList);
        final ControlDto controlDto = getControlService().createControlFrom(requestMessage, notificationDto.getContent().getFromPartyId(), identifiersResultsDto);
        final RequestDto request = createReceivedRequest(controlDto, identifiersDtoList);
        sendRequest(request);
    }

    private void updateExistingControl(final String bodyFromNotification, final ControlEntity existingControl, final NotificationDto notificationDto) {
        final IdentifiersResponseDto response = getSerializeUtils().mapXmlStringToClass(bodyFromNotification, IdentifiersResponseDto.class);
        final List<IdentifiersResultDto> identifiersResultDtos = response.getIdentifiers();
        final IdentifiersResults identifiersResults = buildIdentifiersResultFrom(identifiersResultDtos);
        updateControlIdentifiers(existingControl, identifiersResults, identifiersResultDtos);
        updateControlRequests(existingControl.getRequests(), identifiersResults, notificationDto);
        if (!StatusEnum.ERROR.equals(existingControl.getStatus())) {
            existingControl.setStatus(getControlStatus(existingControl));
        }
        getControlService().save(existingControl);
    }

    private StatusEnum getControlStatus(final ControlEntity existingControl) {
        final StatusEnum currentControlStatus = existingControl.getStatus();
        final List<RequestEntity> requests = existingControl.getRequests();
        if (requests.stream().allMatch(requestEntity -> RequestStatusEnum.SUCCESS == requestEntity.getStatus())) {
            return StatusEnum.COMPLETE;
        } else if (shouldSetTimeout(requests)) {
            return StatusEnum.TIMEOUT;
        } else if (requests.stream().anyMatch(requestEntity -> RequestStatusEnum.ERROR == requestEntity.getStatus())) {
            return StatusEnum.ERROR;
        }
        return currentControlStatus;
    }

    private static boolean shouldSetTimeout(final List<RequestEntity> requests) {
        return requests.stream().anyMatch(requestEntity -> RequestStatusEnum.TIMEOUT == requestEntity.getStatus())
                && requests.stream().noneMatch(requestEntity -> RequestStatusEnum.ERROR == requestEntity.getStatus());
    }

    private void updateControlIdentifiers(final ControlEntity existingControl, final IdentifiersResults identifiersResults, final List<IdentifiersResultDto> identifiersResultDtos) {
        final IdentifiersResults controlIdentifiersResults = existingControl.getIdentifiersResults();
        if (controlIdentifiersResults == null || controlIdentifiersResults.getIdentifiersResult().isEmpty()) {
            existingControl.setIdentifiersResults(identifiersResults);
        } else {
            final ArrayList<IdentifiersResult> currentIdentifiers = new ArrayList<>(controlIdentifiersResults.getIdentifiersResult());
            final List<IdentifiersResult> responseIdentifiers = getMapperUtils().identifierResultDtosToIdentifierEntities(identifiersResultDtos);
            existingControl.setIdentifiersResults(IdentifiersResults.builder().identifiersResult(ListUtils.union(currentIdentifiers, responseIdentifiers)).build());
        }
    }

    private void updateControlRequests(final List<RequestEntity> pendingRequests, final IdentifiersResults identifiersResults, final NotificationDto notificationDto) {
        CollectionUtils.emptyIfNull(pendingRequests).stream()
                .filter(requestEntity -> isRequestWaitingSentNotification(notificationDto, requestEntity))
                .map(IdentifiersRequestEntity.class::cast)
                .forEach(requestEntity -> {
                    requestEntity.setIdentifiersResults(identifiersResults);
                    requestEntity.setStatus(RequestStatusEnum.SUCCESS);
                });
    }

    private static boolean isRequestWaitingSentNotification(final NotificationDto notificationDto, final RequestEntity requestEntity) {
        return RequestStatusEnum.IN_PROGRESS == requestEntity.getStatus()
                && requestEntity.getGateUrlDest() != null
                && requestEntity.getGateUrlDest().equalsIgnoreCase(notificationDto.getContent().getFromPartyId());
    }

    public IdentifiersRequestDto createRequest(final ControlDto controlDto, final RequestStatusEnum status, final List<IdentifiersDto> identifiersDtoList) {
        final IdentifiersRequestDto requestDto = save(buildRequestDto(controlDto, status, identifiersDtoList));
        log.info("Request has been register with controlId : {}", requestDto.getControl().getId());
        return requestDto;
    }

    private RequestDto buildRequestDto(final ControlDto controlDto, final RequestStatusEnum status, final List<IdentifiersDto> identifiersDtoList) {
        return IdentifiersRequestDto.builder()
                .retry(0)
                .control(controlDto)
                .status(status)
                .identifiersResultsDto(buildIdentifiersResultDto(identifiersDtoList))
                .gateUrlDest(controlDto.getFromGateUrl())
                .requestType(RequestType.IDENTIFIER)
                .build();
    }

    private RequestDto createReceivedRequest(final ControlDto controlDto, final List<IdentifiersDto> identifiersDtoList) {
        final RequestDto request = createRequest(controlDto, RECEIVED, identifiersDtoList);
        final ControlDto updatedControl = getControlService().getControlByRequestUuid(controlDto.getRequestUuid());
        if (StatusEnum.COMPLETE == updatedControl.getStatus()) {
            request.setStatus(RequestStatusEnum.RESPONSE_IN_PROGRESS);
        }
        request.setControl(updatedControl);
        return request;
    }

    private SearchWithIdentifiersRequestDto buildIdentifiersRequestDtoFrom(final IdentifiersMessageBodyDto messageBody) {
        return SearchWithIdentifiersRequestDto.builder()
                .vehicleID(messageBody.getVehicleID())
                .isDangerousGoods(messageBody.getIsDangerousGoods())
                .transportMode(messageBody.getTransportMode())
                .vehicleCountry(messageBody.getVehicleCountry())
                .build();
    }

    public IdentifiersResultsDto buildIdentifiersResultDto(final List<IdentifiersDto> identifiersDtos) {
        final List<IdentifiersResultDto> identifiersResultDtos = getMapperUtils().identifierDtosToIdentifiersResultDto(identifiersDtos);
        return IdentifiersResultsDto.builder()
                .identifiersResult(identifiersResultDtos)
                .build();
    }

    public IdentifiersResults buildIdentifiersResult(final List<IdentifiersDto> identifiersDtos) {
        final List<IdentifiersResult> identifiersResults = getMapperUtils().identifierDtosToIdentifierEntities(identifiersDtos);
        return IdentifiersResults.builder()
                .identifiersResult(identifiersResults)
                .build();
    }
    private IdentifiersResults buildIdentifiersResultFrom(final List<IdentifiersResultDto> identifiersResultDtos) {
        final List<IdentifiersResult> identifiersResultList = getMapperUtils().identifierResultDtosToIdentifierEntities(identifiersResultDtos);
        return IdentifiersResults.builder()
                .identifiersResult(identifiersResultList)
                .build();
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
            final IdentifiersResponseDto identifiersResponseDto = getControlService().buildIdentifiersResponse(controlDto);
            return getSerializeUtils().mapObjectToXmlString(identifiersResponseDto);
        } else { //local sending request
            final IdentifiersRequestBodyDto identifiersRequestBodyDto = IdentifiersRequestBodyDto.fromControl(controlDto);
            return getSerializeUtils().mapObjectToXmlString(identifiersRequestBodyDto);
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

    public void updateControlIdentifiers(final ControlDto control, final List<IdentifiersDto> identifiersDtoList) {
        getControlService().getByRequestUuid(control.getRequestUuid()).ifPresent(controlEntity -> {
            if (controlEntity.getIdentifiersResults() == null || controlEntity.getIdentifiersResults().getIdentifiersResult().isEmpty())
            {
                controlEntity.setIdentifiersResults(buildIdentifiersResult(identifiersDtoList));
            } else {
                final ArrayList<IdentifiersResult> existingIdentifiers = new ArrayList<>(controlEntity.getIdentifiersResults().getIdentifiersResult());
                final List<IdentifiersResult> responseIdentifiers = getMapperUtils().identifierDtosToIdentifierEntities(identifiersDtoList);
                controlEntity.setIdentifiersResults(IdentifiersResults.builder().identifiersResult(ListUtils.union(existingIdentifiers, responseIdentifiers)).build());
            }
            getControlService().save(controlEntity);
        });
    }
}
