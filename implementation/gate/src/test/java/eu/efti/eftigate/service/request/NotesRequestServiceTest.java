package eu.efti.eftigate.service.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.NotesRequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.MemoryAppender;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.eftigate.EftiTestUtils;
import eu.efti.eftigate.dto.NoteResponseDto;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.NoteRequestEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.repository.NotesRequestRepository;
import eu.efti.eftigate.service.BaseServiceTest;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.efti.commons.enums.RequestStatusEnum.ERROR;
import static eu.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.RECEIVED;
import static eu.efti.commons.enums.RequestStatusEnum.RESPONSE_IN_PROGRESS;
import static eu.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotesRequestServiceTest extends BaseServiceTest {
    private NotesRequestService notesRequestService;
    @Mock
    private NotesRequestRepository notesRequestRepository;
    @Captor
    ArgumentCaptor<NoteRequestEntity> noteRequestEntityArgumentCaptor;

    private final NoteRequestEntity noteRequestEntity = new NoteRequestEntity();
    private final UilRequestEntity uilRequestEntity = new UilRequestEntity();
    private final NotesRequestDto notesRequestDto = new NotesRequestDto();
    private final ValidationService validationService = new ValidationService();

    @Override
    @BeforeEach
    public void before() {
        super.before();
        super.setDtoRequestCommonAttributes(notesRequestDto);
        super.setEntityRequestCommonAttributes(noteRequestEntity);
        super.setEntityRequestCommonAttributes(uilRequestEntity);

        controlEntity.setRequests(List.of(uilRequestEntity, noteRequestEntity));
        notesRequestService = new NotesRequestService(notesRequestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager, validationService);
        final Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(NotesRequestService.class);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(Level.INFO, memoryAppenderTestLogger);
    }

    @Test
    void manageSendErrorTest() {
        final ErrorDto errorDto = ErrorDto.fromErrorCode(ErrorCodesEnum.AP_SUBMISSION_ERROR);
        final NotesRequestDto requestDtoWithError = NotesRequestDto.builder()
                .error(errorDto)
                .control(ControlDto.builder().error(errorDto).fromGateId("fromGateId").gateId("gateId").build())
                .gateIdDest("gateIdDest")
                .requestType(RequestType.NOTE)
                .build();
        final NoteRequestEntity noteRequestEntityWithError = mapperUtils.requestDtoToRequestEntity(requestDtoWithError, NoteRequestEntity.class);
        Mockito.when(notesRequestRepository.save(any())).thenReturn(noteRequestEntityWithError);

        notesRequestService.manageSendError(requestDtoWithError);

        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(ERROR, noteRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void sendTest() throws JsonProcessingException {
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.createAndSendRequest(controlDto, null);

        verify(notesRequestRepository, Mockito.times(1)).save(any());
        verify(rabbitSenderService, Mockito.times(1)).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void shouldUpdateStatus() {
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);
        notesRequestService.updateStatus(noteRequestEntity, ERROR);
        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        verify(notesRequestRepository, Mockito.times(1)).save(any(NoteRequestEntity.class));
        assertEquals(ERROR, noteRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void allRequestsContainsDataTest() {
        //Act and Assert
        final List<RequestEntity> noteRequestEntityList = List.of(noteRequestEntity);
        assertThrows(UnsupportedOperationException.class, () -> notesRequestService.allRequestsContainsData(noteRequestEntityList));
    }

    @Test
    void shouldManageMessageReceiveAndMarkMessageAsDownloaded_whenControlExists() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .messageId("")
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(EftiTestUtils.testFile("/xml/FTI026.xml"))
                        .fromPartyId("gate")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setGateIdDest("gate");

        controlEntity.setRequests(List.of(uilRequestEntity));
        when(controlService.getByRequestId("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(Optional.of(controlEntity));
        Mockito.when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);
        //Act
        notesRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService, never()).createControlFrom(any(), any());
        verify(rabbitSenderService, times(1)).sendMessageToRabbit(any(), any(), any());
        verify(requestUpdaterService).setMarkedAsDownload(any(), any());
        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals("The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear", noteRequestEntityArgumentCaptor.getValue().getNote());
        assertEquals("note-request-id", noteRequestEntityArgumentCaptor.getValue().getNoteRequestId());
    }

    @Test
    void shouldUpdateRequestStatus_whenRequestSentSuccessfully() {
        noteRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        when(notesRequestRepository.findByStatusAndEdeliveryMessageId(IN_PROGRESS, MESSAGE_ID)).thenReturn(noteRequestEntity);

        notesRequestService.manageSendSuccess(MESSAGE_ID);

        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(SUCCESS, noteRequestEntityArgumentCaptor.getValue().getStatus());
        assertTrue(memoryAppender.containsFormattedLogMessage("sent note message messageId successfully"));
        assertEquals(1, memoryAppender.countEventsForLogger(NotesRequestService.class.getName(), Level.INFO));
    }

    @Test
    void shouldThrowException_whenRequestSentSuccessfullyAndNoteInProgressNotFound() {
        assertThrows(RequestNotFoundException.class, () -> notesRequestService.manageSendSuccess(MESSAGE_ID));
    }

    @Test
    void shouldBuildResponseBody_whenRequestReceived() {
        controlDto.setRequestType(RequestTypeEnum.NOTE_SEND);
        controlDto.setPlatformId("acme");
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setPlatformId("example");
        rabbitRequestDto.setStatus(RECEIVED);
        rabbitRequestDto.setNote("The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear");
        rabbitRequestDto.setNoteRequestId("note-request-id");

        final String expectedRequestBody = EftiTestUtils.testFile("/xml/FTI026.xml");

        final String requestBody = notesRequestService.buildRequestBody(rabbitRequestDto);

        MatcherAssert.assertThat(expectedRequestBody, CompareMatcher.isSimilarTo(requestBody)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    void shouldBuildResponseBody_whenStatusIsResponseInProgress() {
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setStatus(RESPONSE_IN_PROGRESS);
        rabbitRequestDto.setNoteRequestId("note-request-id");

        final String expectedResponseBody = EftiTestUtils.testFile("/xml/FTI030.xml");

        final String requestBody = notesRequestService.buildRequestBody(rabbitRequestDto);

        MatcherAssert.assertThat(expectedResponseBody, CompareMatcher.isSimilarTo(requestBody)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    void shouldBuildResponseBody_whenStatusIsError() {
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setStatus(RequestStatusEnum.ERROR);
        rabbitRequestDto.setNoteRequestId("note-request-id");
        rabbitRequestDto.setError(ErrorDto.builder()
                .errorCode("BAD_REQUEST")
                .errorDescription("Bad request")
                .build());

        final String expectedResponseBody = EftiTestUtils.testFile("/xml/FTI030-error.xml");

        final String requestBody = notesRequestService.buildRequestBody(rabbitRequestDto);

        MatcherAssert.assertThat(expectedResponseBody, CompareMatcher.isSimilarTo(requestBody)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    void manageRestRequestDone_shouldUpdateStatusToSuccess_whenNotExternalAsk() throws JsonProcessingException {
        noteRequestEntity.setRequestType(RequestType.NOTE.name());
        noteRequestEntity.setStatus(IN_PROGRESS);
        controlEntity.setRequestType(RequestTypeEnum.NOTE_SEND);
        when(notesRequestRepository.findByControlRequestIdAndStatus(anyString(), any())).thenReturn(noteRequestEntity);
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.manageRestRequestDone(MESSAGE_ID);

        verify(notesRequestRepository, times(1)).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(SUCCESS, noteRequestEntityArgumentCaptor.getValue().getStatus());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void manageRestRequestDone_shouldRespondToOtherGate_whenExternalAsk() throws JsonProcessingException {
        noteRequestEntity.setRequestType(RequestType.NOTE.name());
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setNoteRequestId("note-request-id");
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        controlEntity.setFromGateId("fromGate");
        when(notesRequestRepository.findByControlRequestIdAndStatus(anyString(), any())).thenReturn(noteRequestEntity);
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.manageRestRequestDone(MESSAGE_ID);

        verify(notesRequestRepository, times(3)).save(any());
        verify(rabbitSenderService, times(1)).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void manageResponseReceive_shouldUpdateStatusToSuccess_whenPlatformResponseOk() throws IOException {
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setNoteRequestId("note-request-id");
        controlEntity.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        final NotificationDto notificationDto = buildFollowUpResponseNotification("/xml/FTI030.xml");
        when(notesRequestRepository.findByNoteRequestIdAndStatus("note-request-id", IN_PROGRESS)).thenReturn(noteRequestEntity);
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.manageResponseReceive(notificationDto);

        verify(notesRequestRepository, times(1)).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(SUCCESS, noteRequestEntityArgumentCaptor.getValue().getStatus());
        verify(requestUpdaterService).setMarkedAsDownload(any(), any());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void manageResponseReceive_shouldRespondToOtherGate_whenPlatformResponseOkAndExternalAsk() throws IOException {
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setNoteRequestId("note-request-id");
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        controlEntity.setFromGateId("fromGate");
        final NotificationDto notificationDto = buildFollowUpResponseNotification("/xml/FTI030.xml");
        when(notesRequestRepository.findByNoteRequestIdAndStatus("note-request-id", IN_PROGRESS)).thenReturn(noteRequestEntity);
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.manageResponseReceive(notificationDto);

        verify(notesRequestRepository, times(3)).save(any());
        verify(rabbitSenderService, times(1)).sendMessageToRabbit(any(), any(), any());
        verify(requestUpdaterService).setMarkedAsDownload(any(), any());
    }

    @Test
    void manageResponseReceive_shouldSetError_whenPlatformResponseNotOk() throws IOException {
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setNoteRequestId("note-request-id");
        controlEntity.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        final NotificationDto notificationDto = buildFollowUpResponseNotification("/xml/FTI030-error.xml");
        when(notesRequestRepository.findByNoteRequestIdAndStatus("note-request-id", IN_PROGRESS)).thenReturn(noteRequestEntity);
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.manageResponseReceive(notificationDto);

        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(ERROR, noteRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void manageResponseReceive_shouldUpdateStatusToSuccess_whenGateResponseOk() throws IOException {
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setNoteRequestId("note-request-id");
        controlEntity.setRequestType(RequestTypeEnum.NOTE_SEND);
        final NotificationDto notificationDto = buildFollowUpResponseNotification("/xml/FTI030.xml");
        when(notesRequestRepository.findByNoteRequestIdAndStatus("note-request-id", IN_PROGRESS)).thenReturn(noteRequestEntity);
        when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);

        notesRequestService.manageResponseReceive(notificationDto);

        verify(notesRequestRepository, times(1)).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(SUCCESS, noteRequestEntityArgumentCaptor.getValue().getStatus());
        verify(rabbitSenderService, never()).sendMessageToRabbit(any(), any(), any());
    }

    @Test
    void manageResponseReceive_shouldDoNothing_whenNoteRequestNotFound() throws IOException {
        final NotificationDto notificationDto = buildFollowUpResponseNotification("/xml/FTI030.xml");
        when(notesRequestRepository.findByNoteRequestIdAndStatus(anyString(), any())).thenReturn(null);

        notesRequestService.manageResponseReceive(notificationDto);

        verify(notesRequestRepository, never()).save(any());
        verify(requestUpdaterService, never()).setMarkedAsDownload(any(), any());
    }

    private NotificationDto buildFollowUpResponseNotification(final String xmlFile) throws IOException {
        return NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .messageId("")
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(EftiTestUtils.testFile(xmlFile))
                        .fromPartyId("gate")
                        .build())
                .build();
    }

    @Test
    void getStatus_shouldReturnStatus_whenNoteRequestExists() {
        noteRequestEntity.setStatus(SUCCESS);
        when(notesRequestRepository.findByNoteRequestId("note-request-id")).thenReturn(noteRequestEntity);

        final NoteResponseDto result = notesRequestService.getStatus("note-request-id");

        assertEquals("note-request-id", result.getRequestId());
        assertEquals(StatusEnum.COMPLETE, result.getStatus());
        assertNull(result.getErrorCode());
    }

    @Test
    void getStatus_shouldReturnNotFoundError_whenNoteRequestDoesNotExist() {
        when(notesRequestRepository.findByNoteRequestId("unknown")).thenReturn(null);

        final NoteResponseDto result = notesRequestService.getStatus("unknown");

        assertEquals(ErrorCodesEnum.ID_NOT_FOUND.name(), result.getErrorCode());
    }

    @Test
    void shouldFindRequestByMessageId_whenRequestExists() {
        when(notesRequestRepository.findByEdeliveryMessageId(anyString())).thenReturn(noteRequestEntity);
        final NoteRequestEntity requestByMessageId = notesRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        assertNotNull(requestByMessageId);
    }

    @Test
    void shouldThrowException_whenFindRequestByMessageId_andRequestDoesNotExists() {
        final Exception exception = assertThrows(RequestNotFoundException.class, () -> {
            notesRequestService.findRequestByMessageIdOrThrow(MESSAGE_ID);
        });
        assertEquals("couldn't find Notes request for messageId: messageId", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRequestTypeEnumSupport")
    void supports_ShouldReturnTrueForUil(final RequestTypeEnum requestTypeEnum, final boolean expectedResult) {
        assertEquals(expectedResult, notesRequestService.supports(requestTypeEnum));
    }

    private static Stream<Arguments> getArgumentsForRequestTypeEnumSupport() {
        return Stream.of(
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_NOTE_SEND, true),
                Arguments.of(RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH, false),
                Arguments.of(RequestTypeEnum.LOCAL_UIL_SEARCH, false)
        );
    }

    @Test
    void findAllForControlId_notSupported() {
        assertThrows(UnsupportedOperationException.class, () -> notesRequestService.findAllForControlId(1));
    }
}

