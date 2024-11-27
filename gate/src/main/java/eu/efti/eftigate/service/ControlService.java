package eu.efti.eftigate.service;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.PostFollowUpRequestDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.ValidableDto;
import eu.efti.commons.dto.identifiers.api.ConsignmentApiDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.NoteResponseDto;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.ErrorEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.AmbiguousIdentifierException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.ControlRepository;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.utils.ControlUtils;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.edelivery.IdentifierQuery;
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

import static eu.efti.commons.enums.ErrorCodesEnum.ID_NOT_FOUND;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.StatusEnum.PENDING;
import static java.lang.String.format;
import static java.util.Collections.emptyList;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class ControlService {

    public static final String ERROR_REQUEST_ID_NOT_FOUND = "Error requestId not found.";
    public static final String NOTE_WAS_NOT_SENT = "note was not sent";
    private final ControlRepository controlRepository;
    private final EftiGateIdResolver eftiGateIdResolver;
    private final IdentifiersService identifiersService;
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
        if (resetUuid) {
            controlDto.setRequestId(null);
        }
        log.error("{}, {}", error.getErrorDescription(), error.getErrorCode());
    }

    public ControlEntity getById(final long id) {
        final Optional<ControlEntity> controlEntity = controlRepository.findById(id);
        return controlEntity.orElse(null);
    }

    @Transactional("controlTransactionManager")
    public RequestIdDto createUilControl(final UilDto uilDto) {
        log.info("create Uil control for dataset id : {}", uilDto.getDatasetId());
        return createControl(uilDto, ControlUtils
                .fromUilControl(uilDto, gateProperties.isCurrentGate(uilDto.getGateId()) ? RequestTypeEnum.LOCAL_UIL_SEARCH : RequestTypeEnum.EXTERNAL_UIL_SEARCH));
    }

    public RequestIdDto createIdentifiersControl(final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        log.info("create Consignment control for identifier : {}", identifiersRequestDto.getIdentifier());
        return createControl(identifiersRequestDto, ControlUtils.fromLocalIdentifiersControl(identifiersRequestDto, RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH));
    }

    public NoteResponseDto createNoteRequestForControl(final PostFollowUpRequestDto postFollowUpRequestDto) {
        log.info("create Note Request for control with requestId : {}", postFollowUpRequestDto.getRequestId());
        final ControlDto savedControl = getControlByRequestId(postFollowUpRequestDto.getRequestId());
        if (savedControl != null && savedControl.isFound()) {
            log.info("sending note to platform {}", savedControl.getPlatformId());
            return createNoteRequestForControl(savedControl, postFollowUpRequestDto);
        } else {
            return new NoteResponseDto(NOTE_WAS_NOT_SENT, ID_NOT_FOUND.name(), ID_NOT_FOUND.getMessage());
        }
    }

    private NoteResponseDto createNoteRequestForControl(final ControlDto controlDto, final PostFollowUpRequestDto notesDto) {
        final Optional<ErrorDto> errorOptional = this.validateControl(notesDto);
        if (errorOptional.isPresent()) {
            final ErrorDto errorDto = errorOptional.get();
            return new NoteResponseDto(NOTE_WAS_NOT_SENT, errorDto.getErrorCode(), errorDto.getErrorDescription());
        } else {
            controlDto.setNotes(notesDto.getMessage());
            getRequestService(RequestTypeEnum.NOTE_SEND).createAndSendRequest(controlDto, !gateProperties.isCurrentGate(controlDto.getGateId()) ? controlDto.getGateId() : null);
            log.info("Note has been registered for control with request uuid '{}'", controlDto.getRequestId());
            return NoteResponseDto.builder().message("Note sent").build();
        }
    }

    private Optional<ErrorDto> validateControl(final ValidableDto validable) {
        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<ValidableDto>> violations = validator.validate(validable);

        if (violations.isEmpty()) {
            return Optional.empty();
        }

        //we manage only one error by control
        final ConstraintViolation<ValidableDto> constraintViolation = violations.iterator().next();

        return Optional.of(ErrorDto.fromErrorCode(ErrorCodesEnum.valueOf(constraintViolation.getMessage())));
    }

    public ControlDto getControlByRequestId(final String requestId) {
        log.info("get ControlEntity with request id : {}", requestId);
        final Optional<ControlEntity> optionalControlEntity = getByRequestId(requestId);
        if (optionalControlEntity.isPresent()) {
            return updateExistingControl(optionalControlEntity.get());
        } else {
            return buildNotFoundControlEntity();
        }
    }

    public Optional<ControlEntity> getByRequestId(final String requestId) {
        return controlRepository.findByRequestId(requestId);
    }

    private ControlDto updateExistingControl(final ControlEntity controlEntity) {
        if (PENDING == controlEntity.getStatus()) {
            return updatePendingControl(controlEntity);
        } else {
            return mapperUtils.controlEntityToControlDto(controlEntity);
        }
    }

    public ControlDto updatePendingControl(final ControlEntity controlEntity) {
        if (hasRequestInProgress(controlEntity)) {
            return mapperUtils.controlEntityToControlDto(controlEntity);
        }
        final RequestService<?> requestService = this.getRequestService(controlEntity.getRequestType());
        final boolean allRequestsContainsData = requestService.allRequestsContainsData(controlEntity.getRequests());
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
        if (hasRequestInError(controlEntity)) {
            controlEntity.setStatus(StatusEnum.ERROR);
        } else if (shouldSetTimeoutTo(controlEntity)) {
            controlEntity.setStatus(StatusEnum.TIMEOUT);
            updateControlRequestsWithTimeoutStatus(controlEntity);
        } else if (PENDING.equals(controlEntity.getStatus())) {
            controlEntity.setStatus(StatusEnum.COMPLETE);
        }
        return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
    }

    private boolean shouldSetTimeoutTo(ControlEntity controlEntity) {
        return getSecondsSinceCreation(controlEntity) > timeoutValue &&
                CollectionUtils.emptyIfNull(controlEntity.getRequests())
                        .stream()
                        .anyMatch(request -> EftiGateConstants.IN_PROGRESS_STATUS.contains(request.getStatus()));
    }

    private void updateControlRequestsWithTimeoutStatus(final ControlEntity controlEntity) {
        controlEntity.getRequests().stream()
                .filter(request -> RequestStatusEnum.IN_PROGRESS.equals(request.getStatus()))
                .toList()
                .forEach(request -> {
                    request.setStatus(RequestStatusEnum.TIMEOUT);
                    final String requestType = request.getRequestType();
                    final RequestDto requestDto = mapperUtils.requestToRequestDto(request, EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(RequestType.valueOf(requestType)));
                    final RequestService<?> requestService = requestServiceFactory.getRequestServiceByRequestType(requestType);
                    requestService.save(requestDto);
                    if (controlEntity.isExternalAsk()) {
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
                .error(buildErrorEntity(ID_NOT_FOUND.name())).build());
    }

    private static ErrorEntity buildErrorEntity(final String errorCode) {
        return ErrorEntity.builder()
                .errorCode(errorCode)
                .errorDescription(ERROR_REQUEST_ID_NOT_FOUND).build();
    }

    public ControlDto createControlFrom(final IdentifierQuery identifierQuery, final String fromGateId, final IdentifiersResultsDto identifiersResultsDto) {
        final ControlDto controlDto = ControlUtils.fromExternalIdentifiersControl(identifierQuery, EXTERNAL_ASK_IDENTIFIERS_SEARCH, fromGateId, gateProperties.getOwner(), identifiersResultsDto);
        return this.save(controlDto);
    }

    public ControlDto save(final ControlDto controlDto) {
        return this.save(mapperUtils.controlDtoToControlEntity(controlDto));
    }

    public ControlDto save(final ControlEntity controlEntity) {
        return mapperUtils.controlEntityToControlDto(controlRepository.save(controlEntity));
    }

    private <T extends ValidableDto> RequestIdDto createControl(final T searchDto, final ControlDto controlDto) {
        this.validateControl(searchDto).ifPresentOrElse(
                error -> createErrorControl(controlDto, error, true),
                () -> createControlFromType(searchDto, controlDto));

        logManager.logAppRequest(controlDto, searchDto, LogManager.FTI_008_FTI_014);
        return buildResponse(controlDto);
    }

    public ControlDto createUilControl(final ControlDto controlDto) {
        if (gateProperties.isCurrentGate(controlDto.getGateId()) && !checkOnLocalRegistry(controlDto)) {
            createErrorControl(controlDto, ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND_ON_REGISTRY), false);
            final ControlDto saveControl = this.save(controlDto);
            //respond with the error
            if (controlDto.isExternalAsk()) {
                getRequestService(controlDto.getRequestType()).createAndSendRequest(saveControl, controlDto.getFromGateId(), RequestStatusEnum.ERROR);
            }
            return saveControl;
        } else {
            final ControlDto saveControl = this.save(controlDto);
            getRequestService(controlDto.getRequestType()).createAndSendRequest(saveControl, null);
            log.info("Uil control with request uuid '{}' has been register", saveControl.getRequestId());
            return saveControl;
        }
    }

    private boolean checkOnLocalRegistry(final ControlDto controlDto) {
        log.info("checking local registry for dataUuid {}", controlDto.getEftiDataUuid());
        //log fti015
        logManager.logRequestRegistry(controlDto, null, LogManager.FTI_015);
        final boolean result = this.identifiersService.existByUIL(controlDto.getEftiDataUuid(), controlDto.getGateId(), controlDto.getPlatformId());
        //log fti016
        logManager.logRequestRegistry(controlDto, String.valueOf(result), LogManager.FTI_016);
        return result;
    }

    private void createIdentifiersControl(final ControlDto controlDto, final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto) {
        final List<String> destinationGatesUrls = eftiGateIdResolver.resolve(searchWithIdentifiersRequestDto);

        controlDto.setRequestType(gateToRequestTypeFunction.apply(destinationGatesUrls));
        final ControlDto saveControl = this.save(controlDto);
        CollectionUtils.emptyIfNull(destinationGatesUrls).forEach(destinationUrl -> {
            if (StringUtils.isBlank(destinationUrl)) {
                getRequestService(saveControl.getRequestType()).createRequest(saveControl, RequestStatusEnum.ERROR);
            } else if (destinationUrl.equalsIgnoreCase(gateProperties.getOwner())) {
                eftiAsyncCallsProcessor.checkLocalRepoAsync(searchWithIdentifiersRequestDto, saveControl);
            } else {
                getRequestService(saveControl.getRequestType()).createAndSendRequest(saveControl, destinationUrl);
                final boolean isCurrentGate = gateProperties.isCurrentGate(destinationUrl);
                logManager.logFromIdentifiersRequestDto(controlDto, searchWithIdentifiersRequestDto, isCurrentGate, isCurrentGate ? controlDto.getPlatformId() : destinationUrl, true, false, LogManager.LOG_FROM_IDENTIFIERS_REQUEST_DTO);
            }
        });
        log.info("Identifier control with request uuid '{}' has been register", saveControl.getRequestId());
    }

    private <T> void createControlFromType(final T searchDto, final ControlDto controlDto) {
        if (searchDto instanceof UilDto) {
            createUilControl(controlDto);
        } else if (searchDto instanceof final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto) {
            createIdentifiersControl(controlDto, searchWithIdentifiersRequestDto);
        }
    }

    public RequestIdDto getControlEntity(final String requestId) {
        final ControlDto controlDto = getControlByRequestId(requestId);
        return buildResponse(controlDto);
    }

    private RequestIdDto buildResponse(final ControlDto controlDto) {
        final RequestIdDto result = RequestIdDto.builder()
                .requestId(controlDto.getRequestId())
                .status(controlDto.getStatus())
                .data(controlDto.getEftiData()).build();
        if (controlDto.isError() && controlDto.getError() != null) {
            result.setErrorDescription(controlDto.getError().getErrorDescription());
            result.setErrorCode(controlDto.getError().getErrorCode());
        }
        if (controlDto.getStatus() != PENDING) { // pending request are not logged
            logManager.logAppResponse(controlDto, result, LogManager.FTI_017);
        }
        return result;
    }

    public int updatePendingControls() {
        final List<ControlEntity> pendingControls = controlRepository.findByCriteria(PENDING, timeoutValue);
        CollectionUtils.emptyIfNull(pendingControls).forEach(this::updatePendingControl);
        return CollectionUtils.isNotEmpty(pendingControls) ? pendingControls.size() : 0;
    }

    public IdentifiersResponseDto getIdentifiersResponse(final String requestId) {
        final ControlDto controlDto = getControlByRequestId(requestId);
        return buildIdentifiersResponse(controlDto);
    }

    private IdentifiersResponseDto buildIdentifiersResponse(final ControlDto controlDto) {
        final IdentifiersResponseDto result = IdentifiersResponseDto.builder()
                .requestId(controlDto.getRequestId())
                .status(controlDto.getStatus())
                .identifiers(getIdentifiersResultDtos(controlDto))
                .build();
        if (controlDto.isError() && controlDto.getError() != null) {
            result.setRequestId(null);
            result.setErrorDescription(controlDto.getError().getErrorDescription());
            result.setErrorCode(controlDto.getError().getErrorCode());
        }
        //log fti017
        logManager.logFromIdentifier(result, controlDto, LogManager.FTI_017);
        return result;
    }

    private List<ConsignmentApiDto> getIdentifiersResultDtos(final ControlDto controlDto) {
        if (controlDto.getIdentifiersResults() != null) {
            return mapperUtils.consignmentDtoToApiDto(controlDto.getIdentifiersResults());
        }
        return emptyList();
    }

    public void setError(final ControlDto controlDto, final ErrorDto errorDto) {
        controlDto.setStatus(StatusEnum.ERROR);
        controlDto.setError(errorDto);
        this.save(controlDto);
    }

    public ControlEntity getControlForCriteria(final String requestId, final RequestStatusEnum requestStatus) {
        Preconditions.checkArgument(requestId != null, "Request Uuid must not be null");
        final List<ControlEntity> controls = controlRepository.findByCriteria(requestId, requestStatus);
        if (CollectionUtils.isNotEmpty(controls)) {
            if (controls.size() > 1) {
                throw new AmbiguousIdentifierException(format("Control with request uuid '%s', and request with status '%s' is not unique, %d controls found!", requestId, requestStatus, controls.size()));
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

    public boolean existsByCriteria(final String requestId) {
        return controlRepository.existsByRequestId(requestId);
    }

    public Optional<ControlEntity> findByRequestId(final String controlRequestId) {
        return controlRepository.findByRequestId(controlRequestId);
    }
}
