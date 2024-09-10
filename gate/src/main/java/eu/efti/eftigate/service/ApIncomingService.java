package eu.efti.eftigate.service;

import eu.efti.commons.dto.IdentifiersDto;
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
import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ApIncomingService {


    private final NotificationService notificationService;
    private final RequestServiceFactory requestServiceFactory;
    private final IdentifiersService identifiersService;
    private final SerializeUtils serializeUtils;
    private final EftiRequestUpdater eftiRequestUpdater;

    public void manageIncomingNotification(final ReceivedNotificationDto receivedNotificationDto) {
         notificationService.consume(receivedNotificationDto).ifPresent(this::rootResponse);
    }

    private void rootResponse(final NotificationDto notificationDto) {

        if (NotificationType.SEND_SUCCESS.equals(notificationDto.getNotificationType())) {
            eftiRequestUpdater.manageSendSuccess(notificationDto, LogManager.FTI_ROOT_RESPONSE_SUCESS);
            return;
        } else if (NotificationType.SEND_FAILURE.equals(notificationDto.getNotificationType())) {
            eftiRequestUpdater.manageSendFailure(notificationDto, LogManager.FTI_SEND_FAIL);
            return;
        }

        final EDeliveryAction action = getAction(notificationDto.getContent());
        final RequestService<?> requestService = getRequestService(action);
        switch (action) {
            case GET_UIL, GET_IDENTIFIERS -> requestService.updateWithResponse(notificationDto);
            case FORWARD_UIL -> requestService.receiveGateRequest(notificationDto);
            case SEND_NOTES -> requestService.manageMessageReceive(notificationDto);
            case UPLOAD_IDENTIFIERS -> identifiersService.createOrUpdate(parseBodyToIdentifiers(notificationDto.getContent()));
            default -> log.warn("unmanaged notification type {}", notificationDto.getContent().getAction());
        }
    }

    private EDeliveryAction getAction(final NotificationContentDto notificationContentDto) {
        final EDeliveryAction action = EDeliveryAction.getFromValue(notificationContentDto.getAction());
        if(action == null) {
            log.error("unknown edelivery action {}", notificationContentDto.getAction());
            throw new TechnicalException("unknown edelivery action " + notificationContentDto.getAction());
        }
        return action;
    }

    private IdentifiersDto parseBodyToIdentifiers(final NotificationContentDto notificationContent) {
        return serializeUtils.mapXmlStringToClass(notificationContent.getBody(), IdentifiersDto.class);
    }

    private RequestService<?> getRequestService(final EDeliveryAction eDeliveryAction) {
        return  requestServiceFactory.getRequestServiceByEdeliveryActionType(eDeliveryAction);
    }
}
