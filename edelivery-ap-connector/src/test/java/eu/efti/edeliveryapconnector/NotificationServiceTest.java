package eu.efti.edeliveryapconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.edeliveryapconnector.dto.MessagingDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.dto.PayloadDto;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Optional;

import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.MESSAGE_ID;
import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.MESSAGING;
import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.PAYLOAD;
import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.SENT_FAILURE;
import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.SENT_SUCCESS;
import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.SUBMIT_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
class NotificationServiceTest {
    AutoCloseable openMocks;
    private NotificationService service;

    @BeforeEach
    void init() {
        openMocks = MockitoAnnotations.openMocks(this);
        service = new NotificationService(new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void shouldConsumeNotificationAndManageFailNotif() throws SendRequestException {
        final String messageId = "id_du_message";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SENT_FAILURE, Map.of(MESSAGE_ID, messageId))).build();
        final Optional<NotificationDto> result  = service.consume(receivedNotificationDto);
        assertTrue(result.isPresent());
        assertEquals(messageId, result.get().getMessageId());
        Assertions.assertEquals(NotificationType.SEND_FAILURE, result.get().getNotificationType());
    }

    @Test
    void shouldConsumeNotificationAndManageSendSuccess() throws SendRequestException {
        final String messageId = "id_du_message";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SENT_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final Optional<NotificationDto> result  = service.consume(receivedNotificationDto);
        assertTrue(result.isPresent());
        assertEquals(messageId, result.get().getMessageId());
        assertEquals(NotificationType.SEND_SUCCESS, result.get().getNotificationType());
    }

    @Test
    void shouldConsumeNotificationAndManageSubmitRequest() {
        final Optional<NotificationDto> notificationDto = service.consume(buildSubmitRequest());
        assertNotNull(notificationDto);
        assertTrue(notificationDto.isPresent());
        assertEquals(NotificationType.RECEIVED, notificationDto.get().getNotificationType());
        assertEquals("f58599a1-0889-11ef-a5ee-0242ac120019@domibus.eu", notificationDto.get().getMessageId());
        assertEquals("application/json", notificationDto.get().getContent().getContentType());
        assertEquals("f58599a1-0889-11ef-a5ee-0242ac120019@domibus.eu", notificationDto.get().getContent().getMessageId());
        assertEquals("http://efti.platform.acme.com", notificationDto.get().getContent().getFromPartyId());
        assertEquals("youpi", notificationDto.get().getContent().getBody());
    }

    @Test
    void shouldDoNothingIfUnknowNotif() throws SendRequestException {
        final String messageId = "id_du_message";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of("random type", Map.of(MESSAGE_ID, messageId))).build();
        final Optional<NotificationDto> notificationDto = service.consume(receivedNotificationDto);
        assertNotNull(notificationDto);
        assertTrue(notificationDto.isEmpty());
    }

    private ReceivedNotificationDto buildSubmitRequest() {
        return ReceivedNotificationDto.builder()
                .header(Map.of(MESSAGING, buildHeader()))
                .body(buildPayload()).build();
    }

    private MessagingDto buildHeader() {
        return MessagingDto.builder()
                .userMessage(MessagingDto.UserMessage.builder()
                        .partyInfo(MessagingDto.PartyInfo.builder()
                                .from(MessagingDto.From.builder()
                                        .partyId(Map.of("", "http://efti.platform.acme.com")).build()).build())
                        .collaborationInfo(MessagingDto.CollaborationInfo.builder()
                                .action("getUIL").build())
                        .messageInfo(MessagingDto.MessageInfo.builder()
                                .messageId("f58599a1-0889-11ef-a5ee-0242ac120019@domibus.eu").build()).build()).build();
    }

    private Map<String, Map<String, Object>> buildPayload() {
        return Map.of(SUBMIT_MESSAGE, Map.of(PAYLOAD, PayloadDto.builder()
                .value("eW91cGk=")
                .mimeType("application/json")
                .payloadId("cid:messageJson").build()));
    }
}
