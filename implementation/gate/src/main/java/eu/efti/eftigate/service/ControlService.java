package eu.efti.eftigate.service;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.IdentifiersResultDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.NotesDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.ValidableDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.IdentifiersMessageBodyDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.NoteResponseDto;
import eu.efti.eftigate.dto.RequestUuidDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.ErrorEntity;
import eu.efti.eftigate.exception.AmbiguousIdentifierException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.ControlRepository;
import eu.efti.eftigate.service.gate.EftiGateUrlResolver;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.utils.ControlUtils;
import eu.efti.identifiersregistry.service.IdentifiersService;
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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static eu.efti.commons.enums.ErrorCodesEnum.UUID_NOT_FOUND;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.StatusEnum.PENDING;
import static java.lang.String.format;
import static java.util.Collections.emptyList;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class ControlService {

    public static final String ERROR_REQUEST_UUID_NOT_FOUND = "Error requestUuid not found.";
    public static final String NOTE_WAS_NOT_SENT = "note was not sent";
    private final ControlRepository controlRepository;
    private final EftiGateUrlResolver eftiGateUrlResolver;
    private final IdentifiersService identifiersService;
    private final MapperUtils mapperUtils;
    private final RequestServiceFactory requestServiceFactory;
    private final LogManager logManager;

    @Value("${efti.control.pending.timeout:60}")
    private Integer timeoutValue;

    private final Function<List<String>, RequestTypeEnum> gateToRequestTypeFunction;

    private final EftiAsyncCallsProcessor eftiAsyncCallsProcessor;

    private final GateProperties gateProperties;

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

    public RequestUuidDto createIdentifiersControl(final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        log.info("create Identifiers control for vehicleId : {}", identifiersRequestDto.getVehicleID());
        return createControl(identifiersRequestDto, ControlUtils.fromLocalIdentifiersControl(identifiersRequestDto, RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH));
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


    public ControlDto createControlFrom(final IdentifiersMessageBodyDto messageBody, final String fromGateUrl, final IdentifiersResultsDto identifiersResultsDto) {
        final ControlDto controlDto = ControlUtils.fromExternalIdentifiersControl(messageBody, EXTERNAL_ASK_IDENTIFIERS_SEARCH, fromGateUrl, gateProperties.getOwner(), identifiersResultsDto);
        return this.save(controlDto);
    }

    private <T extends ValidableDto> RequestUuidDto createControl(final T searchDto, final ControlDto controlDto) {
        this.validateControl(searchDto).ifPresentOrElse(
                error -> createErrorControl(controlDto, error, true),
                () -> createControlFromType(searchDto, controlDto));

        logManager.logAppRequest(controlDto, searchDto);
        return buildResponse(controlDto);
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
        return this.identifiersService.existByUIL(controlDto.getEftiDataUuid(), controlDto.getEftiGateUrl(), controlDto.getEftiPlatformUrl());
    }

    private static void createErrorControl(final ControlDto controlDto, final ErrorDto error, final boolean resetUuid) {
        controlDto.setStatus(StatusEnum.ERROR);
        controlDto.setError(error);
        if(resetUuid) {
            controlDto.setRequestUuid(null);
        }
        log.error(error.getErrorDescription() + ", " + error.getErrorCode());
    }

    private void createIdentifiersControl(final ControlDto controlDto, final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        final List<String> destinationGatesUrls = eftiGateUrlResolver.resolve(identifiersRequestDto);

        controlDto.setRequestType(gateToRequestTypeFunction.apply(destinationGatesUrls));
        final ControlDto saveControl = this.save(controlDto);
        CollectionUtils.emptyIfNull(destinationGatesUrls).forEach(destinationUrl -> {
            if (StringUtils.isBlank(destinationUrl)) {
                getRequestService(saveControl.getRequestType()).createRequest(saveControl, RequestStatusEnum.ERROR);
            } else if (destinationUrl.equalsIgnoreCase(gateProperties.getOwner())){
                eftiAsyncCallsProcessor.checkLocalRepoAsync(identifiersRequestDto, saveControl);
            } else {
                getRequestService(saveControl.getRequestType()).createAndSendRequest(saveControl, destinationUrl);
            }
        });
        log.info("Identifiers control with request uuid '{}' has been register", saveControl.getRequestUuid());
    }

    private <T> void createControlFromType(final T searchDto, final ControlDto controlDto) {
        if(searchDto instanceof UilDto) {
            createUilControl(controlDto);
        } else if (searchDto instanceof final SearchWithIdentifiersRequestDto identifiersRequestDto) {
            createIdentifiersControl(controlDto, identifiersRequestDto);
        }
    }

    public RequestUuidDto getControlEntity(final String requestUuid) {
        final ControlDto controlDto = getControlByRequestUuid(requestUuid);
        return buildResponse(controlDto);
    }

    public int updatePendingControls(){
        final List<ControlEntity> pendingControls = controlRepository.findByCriteria(PENDING.name(), timeoutValue);
        CollectionUtils.emptyIfNull(pendingControls).forEach(this::updatePendingControl);
        return CollectionUtils.isNotEmpty(pendingControls) ? pendingControls.size() : 0;
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
            requestService.setDataFromRequests(controlEntity);
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

    public IdentifiersResponseDto getIdentifiersResponse(final String requestUuid) {
        final ControlDto controlDto = getControlByRequestUuid(requestUuid);
        return buildIdentifiersResponse(controlDto);
    }

    public IdentifiersResponseDto buildIdentifiersResponse(final ControlDto controlDto) {
        final IdentifiersResponseDto result = IdentifiersResponseDto.builder()
                .requestUuid(controlDto.getRequestUuid())
                .status(controlDto.getStatus())
                .identifiers(getIdentifiersResultDtos(controlDto))
                .build();
        if(controlDto.isError()  && controlDto.getError() != null){
            result.setRequestUuid(null);
            result.setErrorDescription(controlDto.getError().getErrorDescription());
            result.setErrorCode(controlDto.getError().getErrorCode());
        }
        return result;
    }

    private List<IdentifiersResultDto> getIdentifiersResultDtos(final ControlDto controlDto) {
        if(controlDto.getIdentifiersResults() != null) {
            return controlDto.getIdentifiersResults().getIdentifiersResult();
        }
        return emptyList();
    }

    private ControlDto handleExistingControlWithoutData(final ControlEntity controlEntity) {
        if (hasRequestInError(controlEntity)){
            controlEntity.setStatus(StatusEnum.ERROR);
        } else if (getSecondsSinceCreation(controlEntity) > timeoutValue) {
            controlEntity.setStatus(StatusEnum.TIMEOUT);
        } else if (PENDING.equals(controlEntity.getStatus())) {
            controlEntity.setStatus(StatusEnum.COMPLETE);
        }
        return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
    }

    private boolean hasRequestInError(final ControlEntity controlEntity) {
        return CollectionUtils.emptyIfNull(controlEntity.getRequests())
                .stream()
                .anyMatch(requestEntity -> RequestStatusEnum.ERROR == requestEntity.getStatus());
    }

    private long getSecondsSinceCreation(final ControlEntity controlEntity) {
        final Instant createdDate = controlEntity.getCreatedDate().toInstant(ZoneOffset.UTC);
        final Duration duration = Duration.between(createdDate, Instant.now());
        return duration.getSeconds();
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

    public void setError(final ControlDto controlDto, final ErrorDto errorDto) {
        controlDto.setStatus(StatusEnum.ERROR);
        controlDto.setError(errorDto);
        this.save(controlDto);
    }

    public ControlDto save(final ControlDto controlDto) {
        return this.save(mapperUtils.controlDtoToControEntity(controlDto));
    }

    public ControlDto save(final ControlEntity controlEntity) {
        return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
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
            logManager.logAppResponse(controlDto, result);
        }
        return result;
    }

    private RequestService<?> getRequestService(final RequestTypeEnum requestType) {
        return  requestServiceFactory.getRequestServiceByRequestType(requestType);
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
}
