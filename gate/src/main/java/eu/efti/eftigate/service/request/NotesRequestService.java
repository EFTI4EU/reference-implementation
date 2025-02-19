package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.NotesRequestDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.NoteRequestEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.NotesRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftigate.service.RabbitSenderService;
import eu.efti.eftigate.service.ValidationService;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.v1.edelivery.PostFollowUpRequest;
import eu.efti.v1.edelivery.UIL;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static eu.efti.commons.constant.EftiGateConstants.NOTES_TYPES;
import static eu.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;

@Slf4j
@Component
public class NotesRequestService extends RequestService<NoteRequestEntity> {

    public static final String NOTE = "NOTE";
    private final NotesRequestRepository notesRequestRepository;

    private final ValidationService validationService;

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
        final ControlDto controlDto = requestDto.getControl();
        final PostFollowUpRequest postFollowUpRequest = new PostFollowUpRequest();
        final UIL uil = new UIL();

        uil.setPlatformId(requestDto.getControl().getPlatformId());
        uil.setGateId(requestDto.getControl().getGateId());
        uil.setDatasetId(controlDto.getDatasetId());
        postFollowUpRequest.setUil(uil);
        postFollowUpRequest.setMessage(requestDto.getNote());
        postFollowUpRequest.setRequestId(requestDto.getControl().getRequestId());


        final JAXBElement<PostFollowUpRequest> note = getObjectFactory().createPostFollowUpRequest(postFollowUpRequest);
        return getSerializeUtils().mapJaxbObjectToXmlString(note, PostFollowUpRequest.class);
    }

    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        throw new UnsupportedOperationException("Operation not allowed for Note Request");
    }

    @Override
    public List<NoteRequestEntity> findAllForControlId(final int controlId) {
        throw new UnsupportedOperationException("Operation not allowed for Note Request");
    }

    private void sendLogNote(final ControlDto controlDto, final boolean isError, final String messageBody) {
        getLogManager().logReceivedNoteMessage(controlDto, ComponentType.GATE, ComponentType.GATE, messageBody, controlDto.getFromGateId(), isError ? StatusEnum.ERROR : StatusEnum.COMPLETE, LogManager.FTI_026);
    }

    public void manageMessageReceive(final NotificationDto notificationDto) {
        NotificationContentDto content = notificationDto.getContent();
        String body = content.getBody();
        try {
            validationService.validateXml(body);
            final PostFollowUpRequest messageBody = getSerializeUtils().mapXmlStringToJaxbObject(body);
            getControlService().getByRequestId(messageBody.getRequestId()).ifPresent(controlEntity -> {
                final ControlDto controlDto = getMapperUtils().controlEntityToControlDto(controlEntity);
                sendLogNote(controlDto, false, body);
                controlDto.setNotes(messageBody.getMessage());
                createAndSendRequest(controlDto, messageBody.getUil().getPlatformId());
                markMessageAsDownloaded(notificationDto.getMessageId());
            });
        } catch (SAXException e) {
            String exceptionMessage = e.getMessage();
            log.error("Received invalid PostFollowUpRequest from {}, {}", content.getFromPartyId(), exceptionMessage);
            sendErrorRequests(notificationDto, exceptionMessage, body);
        } catch (IllegalArgumentException | IOException e) {
            log.error("Something went wrong when validating PostFollowUpRequest from{}", content.getFromPartyId(), e);
            sendErrorRequests(notificationDto, e.getMessage(), body);
        }
    }

    private void sendErrorRequests(final NotificationDto notificationDto, final String e, final String body) {
        RequestDto requestDto = this.buildErrorRequestDto(notificationDto, RequestTypeEnum.EXTERNAL_NOTE_SEND, e, ErrorCodesEnum.XML_ERROR.name());
        sendLogNote(requestDto.getControl(), true, body);
        this.sendRequest(requestDto);
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
    public void saveRequest(final RequestDto requestDto) {
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
}
