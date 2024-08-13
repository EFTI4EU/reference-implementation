package com.ingroupe.efti.eftigate.service.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ingroupe.common.test.log.MemoryAppender;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.ErrorDto;
import com.ingroupe.efti.commons.dto.NotesRequestDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.ErrorCodesEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationContentDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationType;
import com.ingroupe.efti.eftigate.EftiTestUtils;
import com.ingroupe.efti.eftigate.dto.RabbitRequestDto;
import com.ingroupe.efti.eftigate.entity.NoteRequestEntity;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.entity.UilRequestEntity;
import com.ingroupe.efti.eftigate.exception.RequestNotFoundException;
import com.ingroupe.efti.eftigate.repository.NotesRequestRepository;
import com.ingroupe.efti.eftigate.service.BaseServiceTest;
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
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ingroupe.efti.commons.enums.RequestStatusEnum.ERROR;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.IN_PROGRESS;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.RECEIVED;
import static com.ingroupe.efti.commons.enums.RequestStatusEnum.SUCCESS;
import static com.ingroupe.efti.eftigate.EftiTestUtils.testFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Override
    @BeforeEach
    public void before() {
        super.before();
        super.setDtoRequestCommonAttributes(notesRequestDto);
        super.setEntityRequestCommonAttributes(noteRequestEntity);
        super.setEntityRequestCommonAttributes(uilRequestEntity);

        controlEntity.setRequests(List.of(uilRequestEntity, noteRequestEntity));
        notesRequestService = new NotesRequestService(notesRequestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils, logManager);
        final Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(NotesRequestService.class);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(Level.INFO, memoryAppenderTestLogger);
    }

    @Test
    void manageSendErrorTest() {
        final ErrorDto errorDto = ErrorDto.fromErrorCode(ErrorCodesEnum.AP_SUBMISSION_ERROR);
        final NotesRequestDto requestDtoWithError = NotesRequestDto.builder()
                .error(errorDto)
                .control(ControlDto.builder().error(errorDto).fromGateUrl("fromGateUrl").eftiGateUrl("eftiGateUrl").build())
                .gateUrlDest("gateUrlDest")
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
        verify(notesRequestRepository,  Mockito.times(1)).save(any(NoteRequestEntity.class));
        assertEquals(ERROR, noteRequestEntityArgumentCaptor.getValue().getStatus());
    }

    @Test
    void allRequestsContainsDataTest() {
        //Act and Assert
        final List<RequestEntity> noteRequestEntityList = List.of(noteRequestEntity);
        assertThrows(UnsupportedOperationException.class, () -> notesRequestService.allRequestsContainsData(noteRequestEntityList));
    }

    @Test
    void receiveGateRequestTest() {
        final NotificationDto notificationDto = NotificationDto.builder().build();
        //Act and Assert
        assertThrows(UnsupportedOperationException.class, () -> notesRequestService.receiveGateRequest(notificationDto));
    }

    @Test
    void shouldManageMessageReceiveAndMarkMessageAsDownloaded_whenControlExists() throws IOException {
        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .messageId("")
                .content(NotificationContentDto.builder()
                        .messageId(MESSAGE_ID)
                        .body(testFile("/xml/FTI026.xml"))
                        .fromPartyId("gate")
                        .build())
                .build();
        controlEntity.setRequestType(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH);
        noteRequestEntity.setStatus(IN_PROGRESS);
        noteRequestEntity.setGateUrlDest("gate");

        controlEntity.setRequests(List.of(uilRequestEntity));
        when(controlService.getByRequestUuid("67fe38bd-6bf7-4b06-b20e-206264bd639c")).thenReturn(Optional.of(controlEntity));
        Mockito.when(notesRequestRepository.save(any())).thenReturn(noteRequestEntity);
        //Act
        notesRequestService.manageMessageReceive(notificationDto);

        //assert
        verify(controlService, never()).createControlFrom(any(), any(), any());
        verify(rabbitSenderService, times(1)).sendMessageToRabbit(any(), any(), any());
        verify(requestUpdaterService).setMarkedAsDownload(any(), any());
        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals("The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear", noteRequestEntityArgumentCaptor.getValue().getNote());
    }

    @Test
    void shouldUpdateRequestStatus_whenRequestSentSuccessfully() {
        noteRequestEntity.setEdeliveryMessageId(MESSAGE_ID);
        when(notesRequestRepository.findByStatusAndEdeliveryMessageId(IN_PROGRESS, MESSAGE_ID)).thenReturn(noteRequestEntity);

        notesRequestService.manageSendSuccess(MESSAGE_ID);

        verify(notesRequestRepository).save(noteRequestEntityArgumentCaptor.capture());
        assertEquals(SUCCESS, noteRequestEntityArgumentCaptor.getValue().getStatus());
        assertTrue(memoryAppender.containedInFormattedLogMessage("sent note message messageId successfully"));
        assertEquals(1,memoryAppender.countEventsForLogger(NotesRequestService.class.getName(), Level.INFO));
    }

    @Test
    void shouldThrowException_whenRequestSentSuccessfullyAndNoteInProgressNotFound() {
        assertThrows(RequestNotFoundException.class, () -> notesRequestService.manageSendSuccess(MESSAGE_ID));
    }

    @Test
    void shouldBuildResponseBody_whenRequestReceived(){
        controlDto.setRequestType(RequestTypeEnum.NOTE_SEND);
        controlDto.setEftiPlatformUrl("http://efti.platform.acme.com");
        final RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setEFTIPlatformUrl("http://example.com");
        rabbitRequestDto.setStatus(RECEIVED);
        rabbitRequestDto.setNote("The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear");

        final String expectedRequestBody = EftiTestUtils.testFile("/xml/FTI026.xml");

        final String requestBody = notesRequestService.buildRequestBody(rabbitRequestDto);

        MatcherAssert.assertThat(expectedRequestBody, CompareMatcher.isIdenticalTo(requestBody).ignoreWhitespace());
    }

    @Test
    void shouldFindRequestByMessageId_whenRequestExists(){
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
    @MethodSource("getArgumentsForEdeliveryActionSupport")
    void supports_ShouldReturnTrueForUil(final EDeliveryAction eDeliveryAction, final boolean expectedResult) {
        assertEquals(expectedResult, notesRequestService.supports(eDeliveryAction));
    }

    private static Stream<Arguments> getArgumentsForEdeliveryActionSupport() {
        return Stream.of(
                Arguments.of(EDeliveryAction.GET_IDENTIFIERS, false),
                Arguments.of(EDeliveryAction.SEND_NOTES, true),
                Arguments.of(EDeliveryAction.GET_UIL, false),
                Arguments.of(EDeliveryAction.UPLOAD_METADATA, false),
                Arguments.of(EDeliveryAction.FORWARD_UIL, false)
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRequestTypeEnumSupport")
    void supports_ShouldReturnTrueForUil(final RequestTypeEnum requestTypeEnum, final boolean expectedResult) {
        assertEquals(expectedResult, notesRequestService.supports(requestTypeEnum));
    }

    private static Stream<Arguments> getArgumentsForRequestTypeEnumSupport() {
        return Stream.of(
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_METADATA_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_UIL_SEARCH, false),
                Arguments.of(RequestTypeEnum.EXTERNAL_NOTE_SEND, true),
                Arguments.of(RequestTypeEnum.LOCAL_METADATA_SEARCH, false),
                Arguments.of(RequestTypeEnum.LOCAL_UIL_SEARCH, false)
        );
    }
}

