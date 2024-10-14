package eu.efti.platformgatesimulator.service;

import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.service.NotificationService;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(GateProperties.class)
class ApIncomingServiceTest extends AbstractTest {

    AutoCloseable openMocks;

    @Mock
    private RequestSendingService requestSendingService;

    @Mock
    private NotificationService notificationService;
    @Mock
    private GateProperties gateProperties;

    @Mock
    private ReaderService readerService;

    private ApIncomingService apIncomingService;

    @BeforeEach
    public void before() {
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .minSleep(1000)
                .maxSleep(2000)
                .cdaPath("./cda/")
                .ap(GateProperties.ApConfig.builder()
                        .url("url")
                        .password("password")
                        .username("username").build()).build();
        openMocks = MockitoAnnotations.openMocks(this);
        apIncomingService = new ApIncomingService(requestSendingService, notificationService, gateProperties, readerService, xmlMapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void manageIncomingNotificationBadFilesTest() throws IOException, InterruptedException {
        final String body = """
            <UILQuery requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                <uil>
                    <gateId/>
                    <platformId>plateform</platformId>
                    <datasetId>uuid</datasetId>
                </uil>
                <subsetId/>
            </UILQuery>
        """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .action(EDeliveryAction.GET_UIL.getValue())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));
        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());
        verify(readerService).readFromFile(any());
    }

    @Test
    void manageIncomingNotificationTest() throws IOException, InterruptedException {
        final String body = """
            <UILQuery requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                <uil>
                    <gateId/>
                    <platformId>plateform</platformId>
                    <datasetId>uuid</datasetId>
                </uil>
                <subsetId/>
            </UILQuery>
        """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .action(EDeliveryAction.GET_UIL.getValue())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));
        Mockito.when(readerService.readFromFile(any())).thenReturn(new SupplyChainConsignment());
        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());
        verify(readerService).readFromFile(any());
    }
}
