package eu.efti.eftigate.service;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.UilRequestService;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierResponse;
import eu.efti.v1.edelivery.PostFollowUpRequest;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@Slf4j
public class EDeliveryMessageRouter {
    private final Map<Class<?>, Consumer<Object>> routingMap = new HashMap<>();

    public EDeliveryMessageRouter(final UilRequestService uilRequestService,
                                  final IdentifiersRequestService identifiersRequestService,
                                  final NotesRequestService notesRequestService) {
        routingMap.put(UILQuery.class, message -> uilRequestService.manageQueryReceived((NotificationDto) message));
        routingMap.put(UILResponse.class, message -> uilRequestService.manageResponseReceived((NotificationDto) message));
        routingMap.put(IdentifierQuery.class, message -> identifiersRequestService.manageQueryReceived((NotificationDto) message));
        routingMap.put(IdentifierResponse.class, message -> identifiersRequestService.manageResponseReceived((NotificationDto) message));
        routingMap.put(SaveIdentifiersRequest.class, message -> identifiersRequestService.createOrUpdate((NotificationDto) message));
        routingMap.put(PostFollowUpRequest.class, message -> notesRequestService.manageMessageReceive((NotificationDto) message));
    }

    public void process(final NotificationDto notificationDto) {
        resolve(notificationDto).ifPresentOrElse(resolvedEntry -> invoke(notificationDto, resolvedEntry), TechnicalException::new);
    }

    private Optional<Map.Entry<Class<?>, Consumer<Object>>> resolve(final NotificationDto notificationDto) {
        return routingMap.entrySet().stream().filter(entry -> {
            final XmlRootElement rootAnnotation = entry.getKey().getAnnotation(XmlRootElement.class);
            if(rootAnnotation != null) {
                return StringUtils.contains(StringUtils.trim(notificationDto.getContent().getBody()), "<" + rootAnnotation.name());
            }
            return false;
        }).findFirst();
    }

    private void invoke(final NotificationDto notificationDto, final Map.Entry<Class<?>, Consumer<Object>> resolvedEntry) {
        resolvedEntry.getValue().accept(notificationDto);
    }
}
