package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.edeliveryapconnector.dto.ApConfigDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationContentDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationType;
import com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import com.ingroupe.efti.edeliveryapconnector.exception.RetrieveMessageException;
import com.ingroupe.efti.edeliveryapconnector.service.NotificationService;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.exception.TechnicalException;
import com.ingroupe.efti.eftigate.service.request.EftiRequestUpdater;
import com.ingroupe.efti.eftigate.service.request.MetadataRequestService;
import com.ingroupe.efti.eftigate.service.request.RequestServiceFactory;
import com.ingroupe.efti.eftigate.service.request.UilRequestService;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
import com.sun.istack.ByteArrayDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Optional;

import static com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto.MESSAGE_ID;
import static com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto.RECEIVE_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApIncomingServiceTest extends AbstractServiceTest {
    private ApIncomingService service;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private EftiRequestUpdater eftiRequestUpdater;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private MetadataRequestService metadataRequestService;
    @Mock
    private MetadataService metadataService;

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
    private final static String html_body = """
    {
        "eFTIPlatformUrl" : "https://efti.platform.001.eu",
        "eFTIDataUuid" : "ac0bbbc9-f46e-4093-b523-830431fb1001",
        "eFTIGateUrl": "https://efti.gate.001.eu",
        "isDangerousGoods" : true,
        "journeyStart" : "2023-06-11T12:2:00+0000",
        "countryStart" : null,
        "journeyEnd" : "2023-08-13T12:23:00+0000",
        "countryEnd" : "DE",
        "transportVehicles" : [{
            "transportMode" : "tututu",
            "sequence" : 1,
            "vehicleId" : "abc123",
            "vehicleCountry" : "IT",
            "journeyStart" : "2023-06-11T12:23:00+0000",
            "countryStart" : "IT",
            "journeyEnd" : "2023-06-12T12:02:00+0000",
            "countryEnd" : "IT"
        }, {
            "transportMode" : "ROAD",
            "sequence" : 221,
            "vehicleId" : "abc124",
            "vehicleCountry" : null,
            "journeyStart" : "2023-06-12T12:03:00+0000",
            "countryStart" : "gITggggg",
            "journeyEnd" : "2023-08-13T12:02:00+0000",
            "countryEnd" : "DE"
        }]\s
    }
    """;

    @BeforeEach
    public void before() {
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .ap(GateProperties.ApConfig.builder()
                        .url(url)
                        .password(password)
                        .username(username).build()).build();
        service = new ApIncomingService(notificationService, requestServiceFactory, eftiRequestUpdater, metadataService, gateProperties, serializeUtils);
    }

    @Test
    void shouldManageIncomingNotificationForwardUil() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(null)
                        .action(EDeliveryAction.FORWARD_UIL.getValue())
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        when(requestServiceFactory.getRequestServiceByEdeliveryActionType(any())).thenReturn(uilRequestService);
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(apConfigDto, receivedNotificationDto);
        verify(uilRequestService).receiveGateRequest(notificationDto);
    }

    @Test
    void shouldManageIncomingNotification() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(null)
                        .action(EDeliveryAction.GET_UIL.getValue())
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        when(requestServiceFactory.getRequestServiceByEdeliveryActionType(any())).thenReturn(uilRequestService);

        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(apConfigDto, receivedNotificationDto);
        verify(uilRequestService).updateWithResponse(notificationDto);
    }

    @Test
    void shouldManageIncomingNotificationCreateMetadataXml() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(new ByteArrayDataSource(xml_body.getBytes(), "osef"))
                        .action(EDeliveryAction.UPLOAD_METADATA.getValue())
                        .contentType(MediaType.TEXT_XML_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(apConfigDto, receivedNotificationDto);
        verify(metadataService).createOrUpdate(any());
    }

    @Test
    void shouldManageIncomingNotificationCreateMetadataJson() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(new ByteArrayDataSource(html_body.getBytes(), "osef"))
                        .action(EDeliveryAction.UPLOAD_METADATA.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(apConfigDto, receivedNotificationDto);
        verify(metadataService).createOrUpdate(any());
    }

    @Test
    void shouldThrowIfActionNotFound() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(new ByteArrayDataSource(html_body.getBytes(), "osef"))
                        .action("osef")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        assertThrows(TechnicalException.class, () -> service.manageIncomingNotification(receivedNotificationDto));
    }

    @Test
    void shouldThrowIfContentTypeNotFound() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();
        final NotificationDto notificationDto = NotificationDto.builder()
                .content(NotificationContentDto.builder()
                        .messageId(messageId)
                        .body(new ByteArrayDataSource(html_body.getBytes(), "osef"))
                        .action("uploadMetadata")
                        .contentType(MediaType.APPLICATION_CBOR_VALUE)
                        .build())
                .notificationType(NotificationType.RECEIVED)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.of(notificationDto));
        assertThrows(RetrieveMessageException.class, () -> service.manageIncomingNotification(receivedNotificationDto));
    }

    @Test
    void shouldNotUpdateResponseIfNoMessage() {
        final String messageId = "messageId";
        final ReceivedNotificationDto receivedNotificationDto = ReceivedNotificationDto.builder()
                .body(Map.of(RECEIVE_SUCCESS, Map.of(MESSAGE_ID, messageId))).build();
        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .username(username)
                .password(password)
                .url(url)
                .build();

        when(notificationService.consume(apConfigDto, receivedNotificationDto)).thenReturn(Optional.empty());
        service.manageIncomingNotification(receivedNotificationDto);

        verify(notificationService).consume(apConfigDto, receivedNotificationDto);
        verify(uilRequestService, never()).updateWithResponse(any());
    }
}
