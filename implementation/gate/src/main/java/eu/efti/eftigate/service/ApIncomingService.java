package eu.efti.eftigate.service;

import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.service.NotificationService;
import eu.efti.eftigate.service.request.EftiRequestUpdater;
import eu.efti.eftigate.service.request.RequestService;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
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
            case SEND_SUCCESS -> eftiRequestUpdater.manageSendSuccess(notificationDto, LogManager.FTI_ROOT_RESPONSE_SUCESS);
            case SEND_FAILURE -> eftiRequestUpdater.manageSendFailure(notificationDto, LogManager.FTI_SEND_FAIL);
            case RECEIVED -> messageRouter.process(notificationDto);
            default -> log.error("Unhandled notification type: {}", notificationDto.getNotificationType());
        }
    }
}
