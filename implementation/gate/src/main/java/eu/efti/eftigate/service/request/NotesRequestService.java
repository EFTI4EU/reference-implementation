package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.NotesRequestDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.constant.EDeliveryStatus;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.NoteResponseDto;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.NoteRequestEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.NotesRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.v1.edelivery.PostFollowUpRequest;
import eu.efti.v1.edelivery.Response;
import eu.efti.v1.edelivery.UIL;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.NOTES_TYPES;
import static eu.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.LOCAL_UIL_SEARCH;

@Slf4j
@Component
public class NotesRequestService extends RequestService<NoteRequestEntity> {

    public static final String NOTE = "NOTE";
    private final NotesRequestRepository notesRequestRepository;

    private final ValidationService validationService;

    @Value("${efti.control.pending.timeout:60}")
    private Integer timeoutValue;

    public NotesRequestService(final NotesRequestRepository notesRequestRepository,
                               final MapperUtils mapperUtils,
                               final RabbitSenderService rabbitSenderService,
                               final ControlService controlService,
                               final GateProperties gateProperties,
                               final RequestUpdaterService requestUpdaterService,
                               final SerializeUtils serializeUtils,
                               final LogManager logManager,
                               final ValidationService validationService) {
        super(mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        this.notesRequestRepository = notesRequestRepository;
        this.validationService = validationService;
    }

    @Override
    public NotesRequestDto createRequest(final ControlDto controlDto) {
        return new NotesRequestDto(controlDto);
    }

    @Override
    public String buildRequestBody(final RabbitRequestDto requestDto) {
        if (List.of(RequestStatusEnum.RESPONSE_IN_PROGRESS, RequestStatusEnum.ERROR, RequestStatusEnum.TIMEOUT).contains(requestDto.getStatus())) {
            return buildPostFollowUpResponseBody(requestDto);
        }
        return buildPostFollowUpRequestBody(requestDto);
    }

    private String buildPostFollowUpRequestBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        final PostFollowUpRequest postFollowUpRequest = new PostFollowUpRequest();
        final UIL uil = new UIL();

        uil.setPlatformId(controlDto.getPlatformId());
        uil.setGateId(controlDto.getGateId());
        uil.setDatasetId(controlDto.getDatasetId());
        postFollowUpRequest.setUil(uil);
        postFollowUpRequest.setMessage(requestDto.getNote());
        postFollowUpRequest.setRequestId(requestDto.getNoteRequestId());
        postFollowUpRequest.setUilQueryRequestId(controlDto.getRequestId());

        final JAXBElement<PostFollowUpRequest> note = getObjectFactory().createPostFollowUpRequest(postFollowUpRequest);
        return getSerializeUtils().mapJaxbObjectToXmlString(note, PostFollowUpRequest.class);
    }

    private String buildPostFollowUpResponseBody(final RabbitRequestDto requestDto) {
        final ControlDto controlDto = requestDto.getControl();
        final boolean hasError = controlDto.getError() != null || requestDto.getError() != null;
        final Response response = new Response();
        response.setRequestId(requestDto.getNoteRequestId());
        response.setStatus(resolveResponseStatusCode(requestDto, hasError));
        if (hasError) {
            final ErrorDto errorDto = requestDto.getError() != null ? requestDto.getError() : controlDto.getError();
            response.setDescription(errorDto.getErrorDescription());
        }
        final JAXBElement<Response> responseElement = getObjectFactory().createPostFollowUpResponse(response);
        return getSerializeUtils().mapJaxbObjectToXmlString(responseElement, Response.class);
    }

    private String resolveResponseStatusCode(final RabbitRequestDto requestDto, final boolean hasError) {
        if (hasError) {
            return EDeliveryStatus.BAD_REQUEST.getCode();
        } else if (RequestStatusEnum.TIMEOUT.equals(requestDto.getStatus())) {
            return EDeliveryStatus.GATEWAY_TIMEOUT.getCode();
        }
        return EDeliveryStatus.OK.getCode();
    }

    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        throw new UnsupportedOperationException("Operation not allowed for Note Request");
    }

    @Override
    public List<NoteRequestEntity> findAllForControlId(int controlId) {
        throw new UnsupportedOperationException("Operation not allowed for Note Request");
    }

    private void sendLogNote(final ControlDto controlDto, final boolean isError, final String messageBody) {
        final boolean isCurrentGate = getGateProperties().isCurrentGate(controlDto.getGateId());
        final String receiver = isCurrentGate ? controlDto.getPlatformId() : controlDto.getGateId();
        getLogManager().logNoteReceiveFromAapMessage(controlDto,getSerializeUtils().mapObjectToBase64String(messageBody), receiver, ComponentType.GATE, ComponentType.GATE, !isError, RequestTypeEnum.EXTERNAL_NOTE_SEND, LogManager.FTI_026);
    }

    public void manageMessageReceive(final NotificationDto notificationDto) {
        Optional<String> result = validationService.isXmlValid(notificationDto.getContent().getBody());
        if (result.isPresent()) {
            log.error("Received invalid PostFollowUpRequest");
            RequestDto requestDto = this.buildErrorRequestDto(notificationDto, RequestTypeEnum.EXTERNAL_NOTE_SEND, result.get());
            sendLogNote(requestDto.getControl(), true, notificationDto.getContent().getBody());
            this.sendRequest(requestDto);
            return;
        }
        final PostFollowUpRequest messageBody = getSerializeUtils().mapXmlStringToJaxbObject(notificationDto.getContent().getBody());
        getControlService().getByRequestId(messageBody.getUilQueryRequestId()).ifPresent(controlEntity -> {
            final ControlDto controlDto = getMapperUtils().controlEntityToControlDto(controlEntity);
            sendLogNote(controlDto, false, notificationDto.getContent().getBody());
            controlDto.setNotes(messageBody.getMessage());
            // preserve the requesting gate's noteRequestId so its ack can be correlated later
            controlDto.setNoteRequestId(messageBody.getRequestId());
            createAndSendRequest(controlDto, messageBody.getUil().getPlatformId());
            markMessageAsDownloaded(notificationDto.getMessageId());
        });
    }

    public void manageRestRequestInProgress(String requestId) {
        Optional.ofNullable(notesRequestRepository.findByControlRequestIdAndStatus(requestId, RequestStatusEnum.RECEIVED))
                .ifPresentOrElse(
                        uilRequest -> updateStatus(uilRequest, IN_PROGRESS),
                        () -> log.error("Not found request with requestId {}", requestId));
    }

    public void manageRestRequestDone(String requestId) {
        final Optional<NoteRequestEntity> maybeUilRequestDto = Optional.ofNullable(notesRequestRepository.findByControlRequestIdAndStatus(requestId, IN_PROGRESS));
        if (maybeUilRequestDto.isPresent()) {
            if (Objects.equals(RequestType.NOTE.name(), maybeUilRequestDto.get().getRequestType())) {
                NoteRequestEntity uilRequestDto = maybeUilRequestDto.get();
                updateStatus(uilRequestDto, RequestStatusEnum.SUCCESS);
                if (uilRequestDto.getControl().isExternalAsk()) {
                    respondToOtherGate(getMapperUtils().requestToRequestDto(uilRequestDto, NotesRequestDto.class));
                }
            } else {
                throw new IllegalStateException("should only be called for local platform requests");
            }
        } else {
            log.error("couldn't find Notes request for requestId" + ": {}", requestId);
        }
    }

    public void manageResponseReceive(final NotificationDto notificationDto) {
        final String body = notificationDto.getContent().getBody();
        final Optional<String> result = validationService.isXmlValid(body);
        if (result.isPresent()) {
            log.error("Received invalid postFollowUpResponse");
            this.sendRequest(this.buildErrorRequestDto(notificationDto, RequestTypeEnum.EXTERNAL_NOTE_SEND, result.get()));
            return;
        }
        final Response response = getSerializeUtils().mapXmlStringToJaxbObject(body);
        final Optional<NoteRequestEntity> optionalNoteRequestEntity = Optional.ofNullable(
                notesRequestRepository.findByNoteRequestIdAndStatus(response.getRequestId(), IN_PROGRESS));
        if (optionalNoteRequestEntity.isEmpty()) {
            log.error("couldn't find Notes request in progress for noteRequestId: {}", response.getRequestId());
            return;
        }
        final NotesRequestDto foundRequestDto = getMapperUtils().requestToRequestDto(optionalNoteRequestEntity.get(), NotesRequestDto.class);
        if (List.of(LOCAL_UIL_SEARCH, EXTERNAL_ASK_UIL_SEARCH).contains(foundRequestDto.getControl().getRequestType())) {
            handlePlatformResponse(notificationDto, response, foundRequestDto);
        } else {
            handleGateResponse(notificationDto, response, foundRequestDto);
        }
    }

    private void handlePlatformResponse(final NotificationDto notificationDto, final Response response, final NotesRequestDto foundRequestDto) {
        applyResponseStatus(response, foundRequestDto);
        if (foundRequestDto.getControl().isExternalAsk()) {
            respondToOtherGate(foundRequestDto);
        }
        markMessageAsDownloaded(notificationDto.getMessageId());
    }

    private void handleGateResponse(final NotificationDto notificationDto, final Response response, final NotesRequestDto foundRequestDto) {
        applyResponseStatus(response, foundRequestDto);
        markMessageAsDownloaded(notificationDto.getMessageId());
    }

    private void applyResponseStatus(final Response response, final NotesRequestDto foundRequestDto) {
        final EDeliveryStatus status = EDeliveryStatus.fromCode(response.getStatus())
                .orElseThrow(() -> new TechnicalException("status " + response.getStatus() + " not found"));
        if (EDeliveryStatus.OK.equals(status)) {
            updateStatus(foundRequestDto, SUCCESS);
        } else {
            foundRequestDto.setError(ErrorDto.builder()
                    .errorCode(status.name())
                    .errorDescription(response.getDescription())
                    .build());
            updateStatus(foundRequestDto, RequestStatusEnum.ERROR);
        }
    }

    private void respondToOtherGate(final NotesRequestDto notesRequestDto) {
        this.updateStatus(notesRequestDto, RequestStatusEnum.RESPONSE_IN_PROGRESS);
        notesRequestDto.setGateIdDest(notesRequestDto.getControl().getFromGateId());
        final RequestDto saved = this.save(notesRequestDto);
        saved.setRequestType(RequestType.NOTE);
        log.info("Note response {} forwarded to gate {}", notesRequestDto.getNoteRequestId(), notesRequestDto.getGateIdDest());
        this.sendRequest(saved);
    }

    public NoteResponseDto getStatus(final String noteRequestId) {
        final NoteRequestEntity noteRequestEntity = notesRequestRepository.findByNoteRequestId(noteRequestId);
        if (noteRequestEntity == null) {
            return NoteResponseDto.builder()
                    .message("note was not found")
                    .errorCode(ErrorCodesEnum.ID_NOT_FOUND.name())
                    .errorDescription(ErrorCodesEnum.ID_NOT_FOUND.getMessage())
                    .build();
        }
        final NoteResponseDto.NoteResponseDtoBuilder builder = NoteResponseDto.builder()
                .requestId(noteRequestId)
                .status(mapToStatusEnum(noteRequestEntity.getStatus()))
                .message("Note status");
        if (noteRequestEntity.getError() != null) {
            builder.errorCode(noteRequestEntity.getError().getErrorCode())
                    .errorDescription(noteRequestEntity.getError().getErrorDescription());
        }
        return builder.build();
    }

    private StatusEnum mapToStatusEnum(final RequestStatusEnum requestStatus) {
        if (List.of(RequestStatusEnum.RECEIVED, IN_PROGRESS, RequestStatusEnum.RESPONSE_IN_PROGRESS).contains(requestStatus)) {
            return StatusEnum.PENDING;
        } else if (SUCCESS.equals(requestStatus)) {
            return StatusEnum.COMPLETE;
        } else if (RequestStatusEnum.TIMEOUT.equals(requestStatus)) {
            return StatusEnum.TIMEOUT;
        }
        return StatusEnum.ERROR;
    }

    @Override
    public void manageSendSuccess(final String eDeliveryMessageId) {
        final NoteRequestEntity externalRequest = Optional.ofNullable(this.notesRequestRepository.findByStatusAndEdeliveryMessageId(IN_PROGRESS, eDeliveryMessageId))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find Notes request in progress for messageId : " + eDeliveryMessageId));
        log.info(" sent note message {} successfully", eDeliveryMessageId);
        this.updateStatus(externalRequest, SUCCESS);
    }

    @Override
    public boolean supports(final RequestTypeEnum requestTypeEnum) {
        return NOTES_TYPES.contains(requestTypeEnum);
    }

    @Override
    public boolean supports(final String requestType) {
        return NOTE.equalsIgnoreCase(requestType);
    }

    @Override
    public RequestDto save(final RequestDto requestDto) {
        return getMapperUtils().requestToRequestDto(
                notesRequestRepository.save(getMapperUtils().requestDtoToRequestEntity(requestDto, NoteRequestEntity.class)),
                NotesRequestDto.class);
    }

    @Override
    public void saveRequest(RequestDto requestDto) {
        notesRequestRepository.save(getMapperUtils().requestDtoToRequestEntity(requestDto, NoteRequestEntity.class));
    }

    @Override
    protected void updateStatus(final NoteRequestEntity noteRequestEntity, final RequestStatusEnum status) {
        noteRequestEntity.setStatus(status);
        notesRequestRepository.save(noteRequestEntity);
    }

    @Override
    protected NoteRequestEntity findRequestByMessageIdOrThrow(final String eDeliveryMessageId) {
        return Optional.ofNullable(this.notesRequestRepository.findByEdeliveryMessageId(eDeliveryMessageId))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find Notes request for messageId: " + eDeliveryMessageId));
    }

    @Transactional("controlTransactionManager")
    public int timeoutStaleRequests() {
        final LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutValue);
        final List<NoteRequestEntity> staleRequests = notesRequestRepository.findByStatusInAndCreatedDateBefore(
                List.of(RequestStatusEnum.RECEIVED, IN_PROGRESS, RequestStatusEnum.RESPONSE_IN_PROGRESS), cutoff);
        staleRequests.forEach(this::timeoutStaleRequest);
        return staleRequests.size();
    }

    private void timeoutStaleRequest(final NoteRequestEntity noteRequestEntity) {
        updateStatus(noteRequestEntity, RequestStatusEnum.TIMEOUT);
        if (noteRequestEntity.getControl().isExternalAsk()) {
            notifyTimeout(getMapperUtils().requestToRequestDto(noteRequestEntity, NotesRequestDto.class));
        }
    }
}
