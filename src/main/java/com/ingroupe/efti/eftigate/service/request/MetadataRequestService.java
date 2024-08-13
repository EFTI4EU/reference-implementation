package com.ingroupe.efti.eftigate.service.request;

import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.IdentifiersRequestDto;
import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.commons.dto.MetadataResultDto;
import com.ingroupe.efti.commons.dto.MetadataResultsDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.commons.utils.SerializeUtils;
import com.ingroupe.efti.edeliveryapconnector.dto.IdentifiersMessageBodyDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.service.RequestUpdaterService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.dto.RabbitRequestDto;
import com.ingroupe.efti.eftigate.dto.requestbody.MetadataRequestBodyDto;
import com.ingroupe.efti.eftigate.entity.IdentifiersRequestEntity;
import com.ingroupe.efti.eftigate.entity.MetadataResult;
import com.ingroupe.efti.eftigate.entity.MetadataResults;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.exception.RequestNotFoundException;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.repository.IdentifiersRequestRepository;
import com.ingroupe.efti.eftigate.service.ControlService;
import com.ingroupe.efti.eftigate.service.LogManager;
import com.ingroupe.efti.eftigate.service.RabbitSenderService;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.ingroupe.efti.commons.constant.EftiGateConstants.IDENTIFIERS_ACTIONS;
import static com.ingroupe.efti.commons.constant.EftiGateConstants.IDENTIFIERS_TYPES;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.RECEIVED;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Component
public class MetadataRequestService extends RequestService<IdentifiersRequestEntity> {

    public static final String IDENTIFIER = "IDENTIFIER";
    @Lazy
    private final MetadataService metadataService;
    private final IdentifiersRequestRepository identifiersRequestRepository;
    private final IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService;


    public MetadataRequestService(final IdentifiersRequestRepository identifiersRequestRepository,
                                  final MapperUtils mapperUtils,
                                  final RabbitSenderService rabbitSenderService,
                                  final ControlService controlService,
                                  final GateProperties gateProperties,
                                  final MetadataService metadataService,
                                  final RequestUpdaterService requestUpdaterService,
                                  final SerializeUtils serializeUtils,
                                  final LogManager logManager, final IdentifiersControlUpdateDelegateService identifiersControlUpdateDelegateService) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.metadataService = metadataService;
        this.identifiersRequestRepository = identifiersRequestRepository;
        this.identifiersControlUpdateDelegateService = identifiersControlUpdateDelegateService;
    }


    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getMetadataResults()) && isNotEmpty(requestEntity.getMetadataResults().getMetadataResult()));
    }

    @Override
    public void manageMessageReceive(final NotificationDto notificationDto) {
        final String bodyFromNotification = notificationDto.getContent().getBody();
        if (StringUtils.isNotBlank(bodyFromNotification)){
            final String requestUuid = getRequestUuid(bodyFromNotification);
            if (getControlService().existsByCriteria(requestUuid)) {
                identifiersControlUpdateDelegateService.updateExistingControl(bodyFromNotification, requestUuid, notificationDto.getContent().getFromPartyId());
                identifiersControlUpdateDelegateService.setControlNextStatus(requestUuid);
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
            List<MetadataResultDto> metadataResultDtos = new ArrayList<>();
            if (requestDto.getMetadataResults() != null){
                metadataResultDtos = getMapperUtils().metadataResultEntitiesToMetadataResultDtos(requestDto.getMetadataResults().getMetadataResult());
            }
            final MetadataResponseDto metadataResponseDto = getControlService().buildMetadataResponse(controlDto, metadataResultDtos);
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
        final RequestDto updatedRequest = this.updateStatus(request, RequestStatusEnum.RESPONSE_IN_PROGRESS);
        super.sendRequest(updatedRequest);
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
}
