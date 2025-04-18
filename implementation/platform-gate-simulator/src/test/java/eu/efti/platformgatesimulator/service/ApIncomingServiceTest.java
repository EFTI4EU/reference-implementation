package eu.efti.platformgatesimulator.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.efti.commons.utils.MemoryAppender;
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
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(GateProperties.class)
class ApIncomingServiceTest extends AbstractTest {

    AutoCloseable openMocks;

    @Mock
    private IdentifierService identifierService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReaderService readerService;

    @Mock
    private RequestSendingService requestSendingService;

    private ApIncomingService apIncomingService;

    private MemoryAppender memoryAppender;

    private static final String LOGGER_NAME = ApIncomingService.class.getName();

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
        apIncomingService = new ApIncomingService(requestSendingService, notificationService, gateProperties, serializeUtils, readerService, identifierService);
        Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(
                Level.TRACE, memoryAppenderTestLogger);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void manageIncomingNotificationUilResponse() {
        final String body = """
                    <uilResponse
                                xmlns="http://efti.eu/v1/edelivery"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"
                                status="404">
                
                              <description>Consignment FI-AA-31232 does not exist</description>
                    </uilResponse>
                """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));

        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());

        assertTrue(memoryAppender.containsFormattedLogMessage("Receive UilResponse"));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.INFO));
    }

    @Test
    void manageIncomingNotificationIdentifierResponse() {
        final String body = """
                    <identifierResponse
                                xmlns="http://efti.eu/v1/edelivery"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"
                                status="404">
                
                              <description>Consignment FI-AA-31232 does not exist</description>
                    </identifierResponse>
                """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));

        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());

        assertTrue(memoryAppender.containsFormattedLogMessage("Receive IdentifierResponse"));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.INFO));
    }

    @Test
    void manageIncomingNotificationPostFollowUpRequest() {
        final String body = """
                    <postFollowUpRequest
                                requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c"
                                xmlns="http://efti.eu/v1/edelivery"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd">
                              <uil>
                                <gateId>FI1</gateId>
                                <platformId>xxx</platformId>
                                <datasetId>asdf</datasetId>
                              </uil>
                              <message>Identifiers missing</message>
                            </postFollowUpRequest>
                """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));

        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());

        assertTrue(memoryAppender.containsFormattedLogMessage("note \"Identifiers missing\" received for request with id 67fe38bd-6bf7-4b06-b20e-206264bd639c"));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.INFO));
    }

    @Test
    void manageIncomingNotificationIdentifierQuery() {
        final String body = """
                    <identifierQuery xmlns="http://efti.eu/v1/edelivery" xmlns:ns2="http://efti.eu/v1/consignment/identifier" requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                                <identifier>AA123VV</identifier>
                                <registrationCountryCode>BE</registrationCountryCode>
                                <modeCode>ROAD</modeCode>
                            </identifierQuery>
                """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));

        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());

        verify(identifierService, times(1)).sendResponseIdentifier(any(), any());
    }

    @Test
    void manageIncomingNotificationBadFilesTest() throws IOException {
        final String body = """
                    <uilQuery
                            xmlns="http://efti.eu/v1/edelivery"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"
                            status="COMPLETE" requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                       <uil>
                            <datasetId>67fe38bd-6bf7-4b06-b20e-206264bd639c</datasetId>
                       </uil>
                    </uilQuery>
                """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));
        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());
        verify(readerService).readFromFile(any(), anyList());
    }

    @Test
    void manageIncomingNotificationTest() throws IOException {
        final String body = """
                    <uilQuery
                            xmlns="http://efti.eu/v1/edelivery"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"
                            status="COMPLETE" requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                       <uil>
                            <datasetId>67fe38bd-6bf7-4b06-b20e-206264bd639c</datasetId>
                       </uil>
                    </uilQuery>
                """;

        final NotificationDto notificationDto = new NotificationDto();
        notificationDto.setContent(NotificationContentDto.builder()
                .messageId("messageId")
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        Mockito.when(notificationService.consume(any())).thenReturn(Optional.of(notificationDto));
        Mockito.when(readerService.readFromFile(any(), anyList())).thenReturn(new SupplyChainConsignment());
        apIncomingService.manageIncomingNotification(new ReceivedNotificationDto());
        verify(readerService).readFromFile(any(), anyList());
    }
}
