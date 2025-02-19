package eu.efti.eftigate.service;

import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.service.NotificationService;
import eu.efti.eftigate.service.request.EftiRequestUpdater;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.UilRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Optional;

import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.SUBMIT_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApIncomingServiceTest extends BaseServiceTest {
    private ApIncomingService service;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private NotesRequestService notesRequestService;
    @Mock
    private IdentifiersRequestService identifiersRequestService;
    @Mock
    private EftiRequestUpdater eftiRequestUpdater;

    private static final String XML_BODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ns2:saveIdentifiersRequest xmlns:ns2="http://efti.eu/v1/edelivery" xmlns="http://efti.eu/v1/consignment/identifier" datasetId="12345678-ab12-4ab6-8999-123456789abc">
            	<consignment>
            		<carrierAcceptanceDateTime formatId="205">202310011000+0000</carrierAcceptanceDateTime>
            		<deliveryEvent>
            			<actualOccurrenceDateTime formatId="205">202310021500+0000</actualOccurrenceDateTime>
            		</deliveryEvent>
            		<mainCarriageTransportMovement>
            			<dangerousGoodsIndicator>false</dangerousGoodsIndicator>
            			<modeCode>1</modeCode>
            			<usedTransportMeans>
            				<id schemeAgencyId="UN">12345</id>
            				<registrationCountry>
            					<code>DE</code>
            				</registrationCountry>
            			</usedTransportMeans>
            		</mainCarriageTransportMovement>
            		<usedTransportEquipment>
            			<carriedTransportEquipment>
            				<id schemeAgencyId="UN">67890</id>
            				<sequenceNumber>1</sequenceNumber>
            			</carriedTransportEquipment>
            			<categoryCode>AE</categoryCode>
            			<id schemeAgencyId="UN">54321</id>
            			<registrationCountry>
            				<code>FR</code>
            			</registrationCountry>
            			<sequenceNumber>2</sequenceNumber>
            		</usedTransportEquipment>
            	</consignment>
            </ns2:saveIdentifiersRequest>
            """;

    @Override
    @BeforeEach
    public void before() {
        EDeliveryMessageRouter router = new EDeliveryMessageRouter(uilRequestService, identifiersRequestService, notesRequestService);
        service = new ApIncomingService(notificationService, eftiRequestUpdater, router);
    }

    @Test
    void shouldManageIncomingNotificationForwardUil() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body("<UILQuery")
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(uilRequestService).manageQueryReceived(notificationDto);
    }

    @Test
    void shouldManageIncomingNotificationForSaveIdentifierRequest() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body("<saveIdentifiersRequest")
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));

        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(identifiersRequestService).createOrUpdate(notificationDto);
    }

    @Test
    void shouldManageIncomingNotificationCreateIdentifiersXml() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(XML_BODY)
                        .contentType(MediaType.TEXT_XML_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
    }

    @Test
    void shouldManageIncomingNotificationCreateIdentifiersXml_whenSendSuccess() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(XML_BODY)
                        .contentType(MediaType.TEXT_XML_VALUE)
                        .build())
                .notificationType(NotificationType.SEND_SUCCESS)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(eftiRequestUpdater, times(1)).manageSendSuccess(notificationDto, "send sucess to domibus");
    }

    @Test
    void shouldManageIncomingNotificationCreateIdentifiersXml_whenSendFailure() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(XML_BODY)
                        .contentType(MediaType.TEXT_XML_VALUE)
                        .build())
                .notificationType(NotificationType.SEND_FAILURE)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(eftiRequestUpdater, times(1)).manageSendFailure(notificationDto, "send fail to domibus");
    }

    @Test
    void shouldNotUpdateResponseIfNoMessage() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.empty());
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(uilRequestService, never()).manageQueryReceived(any());
    }
}
