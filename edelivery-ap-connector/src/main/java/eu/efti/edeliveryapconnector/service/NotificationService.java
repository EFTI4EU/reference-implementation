package eu.efti.edeliveryapconnector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.edeliveryapconnector.dto.MessagingDto;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.dto.PayloadDto;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.exception.RetrieveMessageException;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationService {

    private final ObjectMapper objectMapper;

    public Optional<NotificationDto> consume(final ReceivedNotificationDto receivedNotificationDto) {
        if (receivedNotificationDto.isSendFailure()) {
            return onSendFailure(receivedNotificationDto);
        } else if (receivedNotificationDto.isSentSuccess()) {
            return onSendSuccess(receivedNotificationDto);
        } else if (receivedNotificationDto.isSubmitMessage()) {
            return onMessageReceived(receivedNotificationDto);
        }

        log.info("received a notification of type {}", receivedNotificationDto.getBody().keySet());
        return Optional.empty();
    }

    private Optional<NotificationDto> onSendFailure(final ReceivedNotificationDto receivedNotificationDto) {
        log.info(" sent message {} failed", receivedNotificationDto.getMessageId().orElse(null));

        return Optional.of(NotificationDto.builder()
                .notificationType(NotificationType.SEND_FAILURE)
                .messageId(receivedNotificationDto.getMessageId().orElse(null)).build());
    }

    private Optional<NotificationDto> onSendSuccess(final ReceivedNotificationDto receivedNotificationDto) {
        log.info(" sent message {} successfull", receivedNotificationDto.getMessageId().orElse(null));

        return Optional.of(NotificationDto.builder()
                .notificationType(NotificationType.SEND_SUCCESS)
                .messageId(receivedNotificationDto.getMessageId().orElse(null)).build());
    }

    private Optional<NotificationDto> onMessageReceived(final ReceivedNotificationDto receivedNotificationDto) {
        final MessagingDto messagingDto = objectMapper.convertValue(receivedNotificationDto.getMessaging(), MessagingDto.class);
        final PayloadDto payloadDto = objectMapper.convertValue(receivedNotificationDto.getPayload(), PayloadDto.class);

        final NotificationContentDto notificationContentDto = NotificationContentDto.builder()
                .body(new String(Base64.getDecoder().decode(payloadDto.getValue().getBytes(UTF_8)), UTF_8))
                .contentType(payloadDto.getMimeType())
                .fromPartyId(messagingDto.getUserMessage().getPartyInfo().getFrom().getPartyId().get(""))
                .messageId(messagingDto.getUserMessage().getMessageInfo().getMessageId())
                .conversationId(messagingDto.getUserMessage().getCollaborationInfo().getConversationId()).build();
        try {
            return Optional.of(NotificationDto.builder()
                    .messageId(messagingDto.getUserMessage().getMessageInfo().getMessageId())
                    .notificationType(NotificationType.RECEIVED)
                    .content(notificationContentDto).build());
        } catch (final SendRequestException e) {
            log.error("error while retrieving message " + receivedNotificationDto.getMessageId());
            throw new RetrieveMessageException("error while retrieving message " + receivedNotificationDto.getMessageId().orElse(null)
                    + " " + e.getMessage());
        }
    }
}
