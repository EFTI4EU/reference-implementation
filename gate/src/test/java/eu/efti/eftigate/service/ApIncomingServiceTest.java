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

    //todo change body
    private static final String XML_BODY = """
            <SaveIdentifiersRequest>
                <eFTIPlatformUrl>https://efti.platform.001.eu</eFTIPlatformUrl>
                <eFTIDataUuid>ac0bbbc9-f46e-4093-b523-830431fb1001</eFTIDataUuid>
                <eFTIGateUrl>https://efti.gate.001.eu"</eFTIGateUrl>
                <isDangerousGoods>true</isDangerousGoods>
                <journeyStart>2023-06-11T12:2:00+0000</journeyStart>
                <countryStart>null</countryStart>
                <journeyEnd>2023-08-13T12:23:00+0000</journeyEnd>
                <countryEnd>DE</countryEnd>
                <transportVehicles>
                    <transportVehicle>
                        <transportMode>tututu</transportMode>
                        <sequence>1</sequence>
                        <vehicleID>abc123</vehicleID>
                        <vehicleCountry>IT</vehicleCountry>
                        <journeyStart>2023-06-11T12:23:00+0000</journeyStart>
                        <countryStart>IT</countryStart>
                        <journeyEnd>2023-06-12T12:02:00+0000</journeyEnd>
                        <countryEnd>IT</countryEnd>
                    </transportVehicle>
                    <transportVehicle>
                        <transportMode>ROAD</transportMode>
                        <sequence>221</sequence>
                        <vehicleID>abc124</vehicleID>
                        <vehicleCountry></vehicleCountry>
                        <journeyStart>2023-06-12T12:03:00+0000</journeyStart>
                        <countryStart>gITggggg</countryStart>
                        <journeyEnd>2023-08-13T12:02:00+0000</journeyEnd>
                        <countryEnd>DE</countryEnd>
                    </transportVehicle>
                </transportVehicles>
            </SaveIdentifiersRequest>
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
