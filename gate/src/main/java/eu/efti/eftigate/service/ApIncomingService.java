package eu.efti.eftigate.service;

import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.service.NotificationService;
import eu.efti.eftigate.service.request.EftiRequestUpdater;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ApIncomingService {


    private final NotificationService notificationService;
    private final EftiRequestUpdater eftiRequestUpdater;
    private final EDeliveryMessageRouter messageRouter;

    public void manageIncomingNotification(final ReceivedNotificationDto receivedNotificationDto) {
        notificationService.consume(receivedNotificationDto).ifPresent(this::rootResponse);
    }

    private void rootResponse(final NotificationDto notificationDto) {
        switch (notificationDto.getNotificationType()) {
            case SEND_SUCCESS -> eftiRequestUpdater.manageSendSuccess(notificationDto);
            case SEND_FAILURE -> eftiRequestUpdater.manageSendFailure(notificationDto);
            case RECEIVED -> messageRouter.process(notificationDto);
            default -> log.error("Unhandled notification type: {}", notificationDto.getNotificationType());
        }
    }
}
