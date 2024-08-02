package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.exception.TechnicalException;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationContentDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationType;
import com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import com.ingroupe.efti.edeliveryapconnector.service.NotificationService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.repository.RequestRepository;
import com.ingroupe.efti.eftigate.service.request.EftiRequestUpdater;
import com.ingroupe.efti.eftigate.service.request.MetadataRequestService;
import com.ingroupe.efti.eftigate.service.request.RequestServiceFactory;
import com.ingroupe.efti.eftigate.service.request.UilRequestService;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Optional;

import static com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto.SUBMIT_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApIncomingServiceTest extends BaseServiceTest {
    private ApIncomingService service;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private RequestRepository<?> requestRepository;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private MetadataRequestService metadataRequestService;
    @Mock
    private MetadataService metadataService;
    @Mock
    private EftiRequestUpdater eftiRequestUpdater;
    @Mock
    private LogManager logManager;
    @Mock
    private GateProperties gateProperties;
    @Mock
    private MapperUtils mapperUtils;

    private final static String url = "url";
    private final static String password = "password";
    private final static String username = "username";
    private final static String xml_body = """
    <metadata>
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
                <vehicleId>abc123</vehicleId>
                <vehicleCountry>IT</vehicleCountry>
                <journeyStart>2023-06-11T12:23:00+0000</journeyStart>
                <countryStart>IT</countryStart>
                <journeyEnd>2023-06-12T12:02:00+0000</journeyEnd>
                <countryEnd>IT</countryEnd>
            </transportVehicle>
            <transportVehicle>
                <transportMode>ROAD</transportMode>
                <sequence>221</sequence>
                <vehicleId>abc124</vehicleId>
                <vehicleCountry></vehicleCountry>
                <journeyStart>2023-06-12T12:03:00+0000</journeyStart>
                <countryStart>gITggggg</countryStart>
                <journeyEnd>2023-08-13T12:02:00+0000</journeyEnd>
                <countryEnd>DE</countryEnd>
            </transportVehicle>
        </transportVehicles>
    </metadata>
    """;

    @Override
    @BeforeEach
    public void before() {
        service = new ApIncomingService(notificationService, requestServiceFactory, metadataService, serializeUtils, eftiRequestUpdater);
    }

    @Test
    void shouldManageIncomingNotificationForwardUil() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(null)
                        .action(EDeliveryAction.FORWARD_UIL.getValue())
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        when(requestServiceFactory.getRequestServiceByEdeliveryActionType(any())).thenReturn(uilRequestService);
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(uilRequestService).receiveGateRequest(notificationDto);
    }

    @Test
    void shouldManageIncomingNotification() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(null)
                        .action(EDeliveryAction.GET_UIL.getValue())
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        when(requestServiceFactory.getRequestServiceByEdeliveryActionType(any())).thenReturn(uilRequestService);

        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(uilRequestService).updateWithResponse(notificationDto);
    }

    @Test
    void shouldManageIncomingNotificationCreateMetadataXml() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(xml_body)
                        .action(EDeliveryAction.UPLOAD_METADATA.getValue())
                        .contentType(MediaType.TEXT_XML_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(metadataService).createOrUpdate(any());
    }

    @Test
    void shouldThrowIfActionNotFound() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(xml_body)
                        .action("osef")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        assertThrows(TechnicalException.class, () -> service.manageIncomingNotification(receivedNotificationDto));
    }

    @Test
    void shouldNotUpdateResponseIfNoMessage() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(SUBMIT_MESSAGE, Map.of(MESSAGE_ID, messageId))).build();

        when(notificationService.consume(receivedNotificationDto)).thenReturn(Optional.empty());
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(receivedNotificationDto);
        verify(uilRequestService, never()).updateWithResponse(any());
    }
}
