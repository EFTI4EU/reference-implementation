package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.MetadataDto;
import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.dto.MetadataResponseDto;
import eu.efti.commons.dto.MetadataResultDto;
import eu.efti.commons.dto.MetadataResultsDto;
import eu.efti.commons.dto.RequestDto;
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
import eu.efti.eftigate.dto.requestbody.MetadataRequestBodyDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.MetadataResult;
import eu.efti.eftigate.entity.MetadataResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.metadataregistry.service.MetadataService;
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
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Component
public class MetadataRequestService extends RequestService<IdentifiersRequestEntity> {

    public static final String IDENTIFIER = "IDENTIFIER";
    @Lazy
    private final MetadataService metadataService;
    private final IdentifiersRequestRepository identifiersRequestRepository;

    public MetadataRequestService(final IdentifiersRequestRepository identifiersRequestRepository,
                                  final MapperUtils mapperUtils,
                                  final RabbitSenderService rabbitSenderService,
                                  final ControlService controlService,
                                  final GateProperties gateProperties,
                                  final MetadataService metadataService,
                                  final RequestUpdaterService requestUpdaterService,
                                  final SerializeUtils serializeUtils,
                                  final LogManager logManager) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.metadataService = metadataService;
        this.identifiersRequestRepository = identifiersRequestRepository;
    }


    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getMetadataResults()) && isNotEmpty(requestEntity.getMetadataResults().getMetadataResult()));
    }

    @Override
    public void setDataFromRequests(final ControlEntity controlEntity) {
        final List<MetadataResult> metadataResultList = controlEntity.getRequests().stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .flatMap(request -> request.getMetadataResults().getMetadataResult().stream())
                .toList();
        controlEntity.setMetadataResults(new MetadataResults(metadataResultList));
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
        final IdentifiersRequestEntity externalRequest = identifiersRequestRepository.findByControlRequestTypeAndStatusAndEdeliveryMessageId(EXTERNAL_ASK_METADATA_SEARCH,
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
        final List<MetadataDto> metadataDtoList = metadataService.search(buildMetadataRequestDtoFrom(requestMessage));
        final MetadataResultsDto metadataResults = buildMetadataResultDto(metadataDtoList);
        final ControlDto controlDto = getControlService().createControlFrom(requestMessage, notificationDto.getContent().getFromPartyId(), metadataResults);
        final RequestDto request = createReceivedRequest(controlDto, metadataDtoList);
        sendRequest(request);
    }

    private void updateExistingControl(final String bodyFromNotification, final ControlEntity existingControl, final NotificationDto notificationDto) {
        final MetadataResponseDto response = getSerializeUtils().mapXmlStringToClass(bodyFromNotification, MetadataResponseDto.class);
        final List<MetadataResultDto> metadataResultDtos = response.getMetadata();
        final MetadataResults metadataResults = buildMetadataResultFrom(metadataResultDtos);
        updateControlMetadata(existingControl, metadataResults, metadataResultDtos);
        updateControlRequests(existingControl.getRequests(), metadataResults, notificationDto);
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

    private void updateControlMetadata(final ControlEntity existingControl, final MetadataResults metadataResults, final List<MetadataResultDto> metadataResultDtos) {
        final MetadataResults controlMetadataResults = existingControl.getMetadataResults();
        if (controlMetadataResults == null || controlMetadataResults.getMetadataResult().isEmpty()) {
            existingControl.setMetadataResults(metadataResults);
        } else {
            final ArrayList<MetadataResult> currentMetadata = new ArrayList<>(controlMetadataResults.getMetadataResult());
            final List<MetadataResult> responseMetadata = getMapperUtils().metadataResultDtosToMetadataEntities(metadataResultDtos);
            existingControl.setMetadataResults(MetadataResults.builder().metadataResult(ListUtils.union(currentMetadata, responseMetadata)).build());
        }
    }

    private void updateControlRequests(final List<RequestEntity> pendingRequests, final MetadataResults metadataResults, final NotificationDto notificationDto) {
        CollectionUtils.emptyIfNull(pendingRequests).stream()
                .filter(requestEntity -> isRequestWaitingSentNotification(notificationDto, requestEntity))
                .map(IdentifiersRequestEntity.class::cast)
                .forEach(requestEntity -> {
                    requestEntity.setMetadataResults(metadataResults);
                    requestEntity.setStatus(RequestStatusEnum.SUCCESS);
                });
    }

    private static boolean isRequestWaitingSentNotification(final NotificationDto notificationDto, final RequestEntity requestEntity) {
        return RequestStatusEnum.IN_PROGRESS == requestEntity.getStatus()
                && requestEntity.getGateUrlDest() != null
                && requestEntity.getGateUrlDest().equalsIgnoreCase(notificationDto.getContent().getFromPartyId());
    }

    public IdentifiersRequestDto createRequest(final ControlDto controlDto, final RequestStatusEnum status, final List<MetadataDto> metadataDtoList) {
        final IdentifiersRequestDto requestDto = save(buildRequestDto(controlDto, status, metadataDtoList));
        log.info("Request has been register with controlId : {}", requestDto.getControl().getId());
        return requestDto;
    }

    private RequestDto buildRequestDto(final ControlDto controlDto, final RequestStatusEnum status, final List<MetadataDto> metadataDtoList) {
        return IdentifiersRequestDto.builder()
                .retry(0)
                .control(controlDto)
                .status(status)
                .metadataResults(buildMetadataResultDto(metadataDtoList))
                .gateUrlDest(controlDto.getFromGateUrl())
                .requestType(RequestType.IDENTIFIER)
                .build();
    }

    private RequestDto createReceivedRequest(final ControlDto controlDto, final List<MetadataDto> metadataDtoList) {
        final RequestDto request = createRequest(controlDto, RECEIVED, metadataDtoList);
        final ControlDto updatedControl = getControlService().getControlByRequestUuid(controlDto.getRequestUuid());
        if (StatusEnum.COMPLETE == updatedControl.getStatus()) {
            request.setStatus(RequestStatusEnum.RESPONSE_IN_PROGRESS);
        }
        request.setControl(updatedControl);
        return request;
    }

    private MetadataRequestDto buildMetadataRequestDtoFrom(final IdentifiersMessageBodyDto messageBody) {
        return MetadataRequestDto.builder()
                .vehicleID(messageBody.getVehicleID())
                .isDangerousGoods(messageBody.getIsDangerousGoods())
                .transportMode(messageBody.getTransportMode())
                .vehicleCountry(messageBody.getVehicleCountry())
                .build();
    }

    public MetadataResultsDto buildMetadataResultDto(final List<MetadataDto> metadataDtos) {
        final List<MetadataResultDto> metadataResultList = getMapperUtils().metadataDtosToMetadataResultDto(metadataDtos);
        return MetadataResultsDto.builder()
                .metadataResult(metadataResultList)
                .build();
    }

    public MetadataResults buildMetadataResult(final List<MetadataDto> metadataDtos) {
        final List<MetadataResult> metadataResultList = getMapperUtils().metadataDtosToMetadataEntities(metadataDtos);
        return MetadataResults.builder()
                .metadataResult(metadataResultList)
                .build();
    }
    private MetadataResults buildMetadataResultFrom(final List<MetadataResultDto> metadataResultDtos) {
        final List<MetadataResult> metadataResultList = getMapperUtils().metadataResultDtosToMetadataEntities(metadataResultDtos);
        return MetadataResults.builder()
                .metadataResult(metadataResultList)
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
        throw new UnsupportedOperationException("Forward Operations not supported for Identifiers");
    }

    @Override
    public IdentifiersRequestDto createRequest(final ControlDto controlDto) {
        return new IdentifiersRequestDto(controlDto);
    }

    @Override
    public String buildRequestBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        if (EXTERNAL_ASK_METADATA_SEARCH == controlDto.getRequestType()) { //remote sending response
            final MetadataResponseDto metadataResponseDto = getControlService().buildMetadataResponse(controlDto);
            return getSerializeUtils().mapObjectToXmlString(metadataResponseDto);
        } else { //local sending request
            final MetadataRequestBodyDto metadataRequestBodyDto = MetadataRequestBodyDto.fromControl(controlDto);
            return getSerializeUtils().mapObjectToXmlString(metadataRequestBodyDto);
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
                .orElseThrow(() -> new RequestNotFoundException("couldn't find Identifiers request for messageId: " + eDeliveryMessageId));
    }

    public void updateControlMetadata(final ControlDto control, final List<MetadataDto> metadataDtoList) {
        getControlService().getByRequestUuid(control.getRequestUuid()).ifPresent(controlEntity -> {
            if (controlEntity.getMetadataResults() == null || controlEntity.getMetadataResults().getMetadataResult().isEmpty())
            {
                controlEntity.setMetadataResults(buildMetadataResult(metadataDtoList));
            } else {
                final ArrayList<MetadataResult> existingMetadata = new ArrayList<>(controlEntity.getMetadataResults().getMetadataResult());
                final List<MetadataResult> responseMetadata = getMapperUtils().metadataDtosToMetadataEntities(metadataDtoList);
                controlEntity.setMetadataResults(MetadataResults.builder().metadataResult(ListUtils.union(existingMetadata, responseMetadata)).build());
            }
            getControlService().save(controlEntity);
        });
    }
}
