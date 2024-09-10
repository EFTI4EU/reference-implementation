package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.constant.EftiGateConstants;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.ErrorDto;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.commons.dto.MetadataResultDto;
import com.ingroupe.efti.commons.dto.MetadataResultsDto;
import com.ingroupe.efti.commons.dto.NotesDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.dto.UilDto;
import com.ingroupe.efti.commons.dto.ValidableDto;
import com.ingroupe.efti.commons.enums.ErrorCodesEnum;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.edeliveryapconnector.dto.IdentifiersMessageBodyDto;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.dto.NoteResponseDto;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.entity.ErrorEntity;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.exception.AmbiguousIdentifierException;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.repository.ControlRepository;
import com.ingroupe.efti.eftigate.service.gate.EftiGateUrlResolver;
import com.ingroupe.efti.eftigate.service.request.RequestService;
import com.ingroupe.efti.eftigate.service.request.RequestServiceFactory;
import com.ingroupe.efti.eftigate.utils.ControlUtils;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.shaded.com.ongres.scram.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.ingroupe.efti.commons.enums.ErrorCodesEnum.UUID_NOT_FOUND;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH;
import static com.ingroupe.efti.commons.enums.StatusEnum.PENDING;
import static java.lang.String.format;
import static java.util.Collections.emptyList;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class ControlService {

    public static final String ERROR_REQUEST_UUID_NOT_FOUND = "Error requestUuid not found.";
    public static final String NOTE_WAS_NOT_SENT = "note was not sent";
    public static final String FTI_008_FTI_014 = "fti008|fti014";
    public static final String FTI_015 = "fti015";
    public static final String FTI_016 = "fti016";
    public static final String LOG_FROM_METADATA_REQUEST_DTO = "logFromMetadataRequestDto";
    public static final String FTI_017 = "fti017";
    private final ControlRepository controlRepository;
    private final EftiGateUrlResolver eftiGateUrlResolver;
    private final MetadataService metadataService;
    private final MapperUtils mapperUtils;
    private final RequestServiceFactory requestServiceFactory;
    private final LogManager logManager;
    private final Function<List<String>, RequestTypeEnum> gateToRequestTypeFunction;
    private final EftiAsyncCallsProcessor eftiAsyncCallsProcessor;
    private final GateProperties gateProperties;
    @Value("${efti.control.pending.timeout:60}")
    private Integer timeoutValue;

    private static void createErrorControl(final ControlDto controlDto, final ErrorDto error, final boolean resetUuid) {
        controlDto.setStatus(StatusEnum.ERROR);
        controlDto.setError(error);
        if(resetUuid) {
            controlDto.setRequestUuid(null);
        }
        log.error("{}, {}", error.getErrorDescription(), error.getErrorCode());
    }

    public ControlEntity getById(final long id) {
        final Optional<ControlEntity> controlEntity = controlRepository.findById(id);
        return controlEntity.orElse(null);
    }

    @Transactional("controlTransactionManager")
    public RequestUuidDto createUilControl(final UilDto uilDto) {
        log.info("create Uil control for uuid : {}", uilDto.getEFTIDataUuid());
        return createControl(uilDto, ControlUtils
                .fromUilControl(uilDto, gateProperties.isCurrentGate(uilDto.getEFTIGateUrl()) ? RequestTypeEnum.LOCAL_UIL_SEARCH : RequestTypeEnum.EXTERNAL_UIL_SEARCH));
    }

    public RequestUuidDto createMetadataControl(final MetadataRequestDto metadataRequestDto) {
        log.info("create metadata control for vehicleID : {}", metadataRequestDto.getVehicleID());
        return createControl(metadataRequestDto, ControlUtils.fromLocalMetadataControl(metadataRequestDto, RequestTypeEnum.LOCAL_METADATA_SEARCH));
    }

    public NoteResponseDto createNoteRequestForControl(final NotesDto notesDto) {
        log.info("create Note Request for control with data uuid : {}", notesDto.getEFTIDataUuid());
        final ControlDto savedControl = getControlByRequestUuid(notesDto.getRequestUuid());
        if (savedControl != null && savedControl.isFound()) {
            return createNoteRequestForControl(savedControl, notesDto);
        } else {
            return new NoteResponseDto(NOTE_WAS_NOT_SENT, UUID_NOT_FOUND.name(), UUID_NOT_FOUND.getMessage());
        }
    }

    private NoteResponseDto createNoteRequestForControl(final ControlDto controlDto, final NotesDto notesDto) {
        final Optional<ErrorDto> errorOptional = this.validateControl(notesDto);
        if (errorOptional.isPresent()){
            final ErrorDto errorDto = errorOptional.get();
            return new NoteResponseDto(NOTE_WAS_NOT_SENT, errorDto.getErrorCode(), errorDto.getErrorDescription());
        } else {
            controlDto.setNotes(notesDto.getNote());
            getRequestService(RequestTypeEnum.NOTE_SEND).createAndSendRequest(controlDto, !gateProperties.isCurrentGate(controlDto.getEftiGateUrl()) ? controlDto.getEftiGateUrl() : null);
            log.info("Note has been registered for control with request uuid '{}'", controlDto.getRequestUuid());
            return  NoteResponseDto.builder().message("Note sent").build();
        }
    }

    private Optional<ErrorDto> validateControl(final ValidableDto validable) {
        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<ValidableDto>> violations = validator.validate(validable);

        if(violations.isEmpty()) {
            return Optional.empty();
        }

        //we manage only one error by control
        final ConstraintViolation<ValidableDto> constraintViolation = violations.iterator().next();

        return Optional.of(ErrorDto.fromErrorCode(ErrorCodesEnum.valueOf(constraintViolation.getMessage())));
    }

    public ControlDto getControlByRequestUuid(final String requestUuid) {
        log.info("get ControlEntity with uuid : {}", requestUuid);
        final Optional<ControlEntity> optionalControlEntity = getByRequestUuid(requestUuid);
        if (optionalControlEntity.isPresent()) {
            return updateExistingControl(optionalControlEntity.get());
        } else {
            return buildNotFoundControlEntity();
        }
    }

    public Optional<ControlEntity> getByRequestUuid(final String requestUuid) {
        return controlRepository.findByRequestUuid(requestUuid);
    }

    public ControlDto updateExistingControl(final ControlEntity controlEntity) {
        if (PENDING == controlEntity.getStatus()) {
            return updatePendingControl(controlEntity);
        } else{
            return mapperUtils.controlEntityToControlDto(controlEntity);
        }
    }

    public ControlDto updatePendingControl(final ControlEntity controlEntity) {
        if (hasRequestInProgress(controlEntity)){
            return mapperUtils.controlEntityToControlDto(controlEntity);
        }
        final RequestService<?> requestService = this.getRequestService(controlEntity.getRequestType());
        final boolean allRequestsContainsData =  requestService.allRequestsContainsData(controlEntity.getRequests());
        if (allRequestsContainsData) {
            controlEntity.setStatus(StatusEnum.COMPLETE);
            return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
        } else {
            return handleExistingControlWithoutData(controlEntity);
        }
    }

    private boolean hasRequestInProgress(final ControlEntity controlEntity) {
        return getSecondsSinceCreation(controlEntity) < timeoutValue &&
                CollectionUtils.emptyIfNull(controlEntity.getRequests()).stream().anyMatch(request -> EftiGateConstants.IN_PROGRESS_STATUS.contains(request.getStatus()));
    }

    private long getSecondsSinceCreation(final ControlEntity controlEntity) {
        return ChronoUnit.SECONDS.between(controlEntity.getCreatedDate(), LocalDateTime.now());
    }

    private ControlDto handleExistingControlWithoutData(final ControlEntity controlEntity) {
        if (hasRequestInError(controlEntity)){
            controlEntity.setStatus(StatusEnum.ERROR);
        } else if (getSecondsSinceCreation(controlEntity) > timeoutValue &&
                CollectionUtils.emptyIfNull(controlEntity.getRequests())
                        .stream()
                        .anyMatch(request -> EftiGateConstants.IN_PROGRESS_STATUS.contains(request.getStatus()))) {
            controlEntity.setStatus(StatusEnum.TIMEOUT);
            updateControlRequests(controlEntity);
        } else if (PENDING.equals(controlEntity.getStatus())) {
            controlEntity.setStatus(StatusEnum.COMPLETE);
        }
        return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
    }

    private void updateControlRequests(final ControlEntity controlEntity) {
        controlEntity.getRequests().stream()
                .filter(request -> RequestStatusEnum.IN_PROGRESS.equals(request.getStatus()))
                .toList()
                .forEach(request -> {
                    request.setStatus(RequestStatusEnum.TIMEOUT);
                    final String requestType = request.getRequestType();
                    final RequestDto requestDto = mapperUtils.requestToRequestDto(request, EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(RequestType.valueOf(requestType)));
                    final RequestService<?> requestService = requestServiceFactory.getRequestServiceByRequestType(requestType);
                    requestService.save(requestDto);
                    if (controlEntity.isExternalAsk()){
                        requestService.notifyTimeout(requestDto);
                    }
                });
    }

    private boolean hasRequestInError(final ControlEntity controlEntity) {
        return CollectionUtils.emptyIfNull(controlEntity.getRequests())
                .stream()
                .anyMatch(requestEntity -> RequestStatusEnum.ERROR == requestEntity.getStatus());
    }

    private RequestService<?> getRequestService(final RequestTypeEnum requestType) {
        return requestServiceFactory.getRequestServiceByRequestType(requestType);
    }

    private ControlDto buildNotFoundControlEntity() {
        return mapperUtils.controlEntityToControlDto(ControlEntity.builder()
                .status(StatusEnum.ERROR)
                .error(buildErrorEntity(UUID_NOT_FOUND.name())).build());
    }

    private static ErrorEntity buildErrorEntity(final String errorCode) {
        return ErrorEntity.builder()
                .errorCode(errorCode)
                .errorDescription(ERROR_REQUEST_UUID_NOT_FOUND).build();
    }

    public ControlDto createControlFrom(final IdentifiersMessageBodyDto messageBody, final String fromGateUrl, final MetadataResultsDto metadataResults) {
        final ControlDto controlDto = ControlUtils.fromExternalMetadataControl(messageBody, EXTERNAL_ASK_METADATA_SEARCH, fromGateUrl, gateProperties.getOwner(), metadataResults);
        return this.save(controlDto);
    }

    public ControlDto save(final ControlDto controlDto) {
        return this.save(mapperUtils.controlDtoToControlEntity(controlDto));
    }

    public ControlDto save(final ControlEntity controlEntity) {
        return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
    }

    private <T extends ValidableDto> RequestUuidDto createControl(final T searchDto, final ControlDto controlDto) {
        this.validateControl(searchDto).ifPresentOrElse(
                error -> createErrorControl(controlDto, error, true),
                () -> createControlFromType(searchDto, controlDto));

        logManager.logAppRequest(controlDto, searchDto, FTI_008_FTI_014);
        return buildResponse(controlDto);
    }

    public ControlDto createUilControl(final ControlDto controlDto) {
        if(gateProperties.isCurrentGate(controlDto.getEftiGateUrl()) && !checkOnLocalRegistry(controlDto)) {
            createErrorControl(controlDto, ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY), false);
            final ControlDto saveControl = this.save(controlDto);
            //respond with the error
            if(controlDto.isExternalAsk()) {
                getRequestService(controlDto.getRequestType()).createAndSendRequest(saveControl, controlDto.getFromGateUrl(), RequestStatusEnum.ERROR);
            }
            return saveControl;
        } else {
            final ControlDto saveControl = this.save(controlDto);
            getRequestService(controlDto.getRequestType()).createAndSendRequest(saveControl, null);
            log.info("Uil control with request uuid '{}' has been register", saveControl.getRequestUuid());
            return saveControl;
        }
    }

    private boolean checkOnLocalRegistry(final ControlDto controlDto) {
        log.info("checking local registry for dataUuid {}", controlDto.getEftiDataUuid());
        //juju commentaire fti015
        logManager.logRequestRegistry(controlDto, null, FTI_015);
        final boolean result = this.metadataService.existByUIL(controlDto.getEftiDataUuid(), controlDto.getEftiGateUrl(), controlDto.getEftiPlatformUrl());
        //juju commentaire fti016
        logManager.logRequestRegistry(controlDto, String.valueOf(result), FTI_016);
        return result;
    }

    private void createMetadataControl(final ControlDto controlDto, final MetadataRequestDto metadataRequestDto) {
        final List<String> destinationGatesUrls = eftiGateUrlResolver.resolve(metadataRequestDto);

        controlDto.setRequestType(gateToRequestTypeFunction.apply(destinationGatesUrls));
        final ControlDto saveControl = this.save(controlDto);
        CollectionUtils.emptyIfNull(destinationGatesUrls).forEach(destinationUrl -> {
            if (StringUtils.isBlank(destinationUrl)) {
                getRequestService(saveControl.getRequestType()).createRequest(saveControl, RequestStatusEnum.ERROR);
            } else if (destinationUrl.equalsIgnoreCase(gateProperties.getOwner())){
                eftiAsyncCallsProcessor.checkLocalRepoAsync(metadataRequestDto, saveControl);
            } else {
                getRequestService(saveControl.getRequestType()).createAndSendRequest(saveControl, destinationUrl);
                final boolean isCurrentGate = gateProperties.isCurrentGate(destinationUrl);
                logManager.logFromMetadataRequestDto(controlDto, metadataRequestDto, isCurrentGate, isCurrentGate ? controlDto.getEftiPlatformUrl() : destinationUrl, true, false, LOG_FROM_METADATA_REQUEST_DTO);
            }
        });
        log.info("Metadata control with request uuid '{}' has been register", saveControl.getRequestUuid());
    }

    private <T> void createControlFromType(final T searchDto, final ControlDto controlDto) {
        if(searchDto instanceof UilDto) {
            createUilControl(controlDto);
        } else if (searchDto instanceof final MetadataRequestDto metadataRequestDto) {
            createMetadataControl(controlDto, metadataRequestDto);
        }
    }

    public RequestUuidDto getControlEntity(final String requestUuid) {
        final ControlDto controlDto = getControlByRequestUuid(requestUuid);
        return buildResponse(controlDto);
    }

    private RequestUuidDto buildResponse(final ControlDto controlDto) {
        final RequestUuidDto result = RequestUuidDto.builder()
                .requestUuid(controlDto.getRequestUuid())
                .status(controlDto.getStatus())
                .eFTIData(controlDto.getEftiData()).build();
        if(controlDto.isError() && controlDto.getError() != null) {
            result.setErrorDescription(controlDto.getError().getErrorDescription());
            result.setErrorCode(controlDto.getError().getErrorCode());
        }
        if(controlDto.getStatus() != PENDING) { // pending request are not logged
            logManager.logAppResponse(controlDto, result, FTI_017);
        }
        return result;
    }

    public int updatePendingControls(){
        final List<ControlEntity> pendingControls = controlRepository.findByCriteria(PENDING, timeoutValue);
        CollectionUtils.emptyIfNull(pendingControls).forEach(this::updatePendingControl);
        return CollectionUtils.isNotEmpty(pendingControls) ? pendingControls.size() : 0;
    }

    public MetadataResponseDto getMetadataResponse(final String requestUuid) {
        final ControlDto controlDto = getControlByRequestUuid(requestUuid);
        return buildMetadataResponse(controlDto);
    }

    public MetadataResponseDto buildMetadataResponse(final ControlDto controlDto) {
        final MetadataResponseDto result = MetadataResponseDto.builder()
                .requestUuid(controlDto.getRequestUuid())
                .status(controlDto.getStatus())
                .metadata(getMetadataResultDtos(controlDto))
                .build();
        if(controlDto.isError()  && controlDto.getError() != null){
            result.setRequestUuid(null);
            result.setErrorDescription(controlDto.getError().getErrorDescription());
            result.setErrorCode(controlDto.getError().getErrorCode());
        }
        //juju commentaire pour fti017
        logManager.logFromMetadata(result, controlDto, FTI_017);
        return result;
    }

    public MetadataResponseDto buildMetadataResponse(final ControlDto controlDto, final List<MetadataResultDto> metadata) {
        final MetadataResponseDto metadataResponseDto = this.buildMetadataResponse(controlDto);
        if (metadata != null && metadataResponseDto.getMetadata().isEmpty()) {
            metadataResponseDto.setMetadata(metadata);
        }
        return metadataResponseDto;
    }

    private List<MetadataResultDto> getMetadataResultDtos(final ControlDto controlDto) {
        if(controlDto.getMetadataResults() != null) {
            return controlDto.getMetadataResults().getMetadataResult();
        }
        return emptyList();
    }

    public void setError(final ControlDto controlDto, final ErrorDto errorDto) {
        controlDto.setStatus(StatusEnum.ERROR);
        controlDto.setError(errorDto);
        this.save(controlDto);
    }

    public ControlEntity getControlForCriteria(final String requestUuid, final RequestStatusEnum requestStatus) {
        Preconditions.checkArgument(requestUuid != null, "Request Uuid must not be null");
        final List<ControlEntity> controls = controlRepository.findByCriteria(requestUuid, requestStatus);
        if (CollectionUtils.isNotEmpty(controls)) {
            if (controls.size() > 1) {
                throw new AmbiguousIdentifierException(format("Control with request uuid '%s', and request with status '%s' is not unique, %d controls found!", requestUuid, requestStatus, controls.size()));
            } else {
                return controls.get(0);
            }
        }
        return null;
    }

    public StatusEnum getControlNextStatus(final ControlEntity existingControl) {
        final List<RequestEntity> requests = existingControl.getRequests();
        if (requests.stream().allMatch(requestEntity -> RequestStatusEnum.SUCCESS == requestEntity.getStatus())) {
            return StatusEnum.COMPLETE;
        } else if (shouldBeTimeout(existingControl)) {
            return StatusEnum.TIMEOUT;
        } else if (requests.stream().anyMatch(requestEntity -> RequestStatusEnum.ERROR == requestEntity.getStatus())) {
            return StatusEnum.ERROR;
        }
        return existingControl.getStatus();
    }


    private boolean shouldBeTimeout(final ControlEntity controlEntity) {
        final Collection<RequestEntity> requests = CollectionUtils.emptyIfNull(controlEntity.getRequests());
        return requests.stream().anyMatch(requestEntity -> RequestStatusEnum.TIMEOUT == requestEntity.getStatus())
                && requests.stream().noneMatch(requestEntity -> RequestStatusEnum.ERROR == requestEntity.getStatus());
    }

    public boolean existsByCriteria(final String requestUuid) {
        return controlRepository.existsByRequestUuid(requestUuid);
    }

    public Optional<ControlEntity> findByRequestUuid(final String controlRequestUuid) {
        return controlRepository.findByRequestUuid(controlRequestUuid);
    }
}
