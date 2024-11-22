package eu.efti.eftigate.service;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.UilRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EDeliveryMessageRouterTest {

    private EDeliveryMessageRouter router;

    @Mock
    private UilRequestService uilRequestService;

    @Mock
    private IdentifiersRequestService identifiersRequestService;

    @Mock
    private NotesRequestService notesRequestService;

    private final NotificationDto notificationDto = new NotificationDto();
    private final Map<String, Consumer<NotificationDto>> casesMap = buildHashMap();

    @BeforeEach
    public void before() {
        router = new EDeliveryMessageRouter(uilRequestService, identifiersRequestService, notesRequestService);
    }

    @Test
    void shouldRouteMessage() {
        casesMap.forEach((key, value) -> {
            notificationDto.setContent(NotificationContentDto.builder().body(key).build());
            assertDoesNotThrow(() -> router.process(notificationDto));
            value.accept(notificationDto);
        });
    }

    @Test
    void shouldThrowExceptionIfIncorrectMessage() {
        notificationDto.setContent(NotificationContentDto.builder().body("<tutu></tutu").build());
        assertThrows(TechnicalException.class, () -> router.process(notificationDto));
    }

    private Map<String, Consumer<NotificationDto>> buildHashMap() {
        final Map<String, Consumer<NotificationDto>> casesMap = new HashMap<>();
        casesMap.put("<UILQuery></UILQuery>", message -> verify(uilRequestService).manageQueryReceived(message));
        casesMap.put("<UILResponse></UILResponse>", message -> verify(uilRequestService).manageResponseReceived(message));
        casesMap.put("<IdentifierQuery></IdentifierQuery>", message -> verify(identifiersRequestService).manageQueryReceived(message));
        casesMap.put("<IdentifierResponse></IdentifierResponse>", message -> verify(identifiersRequestService).manageResponseReceived(message));
        casesMap.put("<SaveIdentifiersRequest></SaveIdentifiersRequest>", message -> verify(identifiersRequestService).createOrUpdate(message));
        casesMap.put("<PostFollowUpRequest></PostFollowUpRequest>", message -> verify(notesRequestService).manageMessageReceive(message));
        return casesMap;
    }
}
