package eu.efti.eftigate.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.MemoryAppender;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.service.request.UilRequestService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RabbitListenerServiceTest extends BaseServiceTest {
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private ApIncomingService apIncomingService;
    @Mock
    private GateIntegrationService gateIntegrationService;
    @Mock
    private PlatformIntegrationService platformIntegrationService;

    private static final String URL = "url";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";

    private RabbitListenerService rabbitListenerService;

    private MemoryAppender memoryAppender;

    private Logger memoryAppenderTestLogger;

    private static final String LOGGER_NAME = RabbitListenerService.class.getName();

    @Override
    @BeforeEach
    public void before() {

        final GateProperties gateProperties = GateProperties.builder()
                .owner("http://france.lol")
                .ap(GateProperties.ApConfig.builder()
                        .url(URL)
                        .password(PASSWORD)
                        .username(USERNAME).build()).build();

        rabbitListenerService = new RabbitListenerService(gateProperties, serializeUtils,
                requestServiceFactory, apIncomingService, gateIntegrationService, platformIntegrationService);
        memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(
                Level.TRACE, memoryAppenderTestLogger);
    }

    @AfterEach
    public void cleanupLogAppenderForTest() {
        MemoryAppender.shutdownMemoryAppender(memoryAppender, memoryAppenderTestLogger);
    }


    @Test
    void listenMessageReceiveDeadQueueTest() {
        final String message = "oki";

        rabbitListenerService.listenMessageReceiveDeadQueue(message);

        assertTrue(memoryAppender.containsFormattedLogMessage(message));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.ERROR));
    }

    @Test
    void listenReceiveMessageTest() {
        final String message = "{\"id\":0,\"journeyStart\":\"2024-01-26T10:54:51+01:00\",\"countryStart\":\"FR\",\"journeyEnd\":\"2024-01-27T10:54:51+01:00\",\"countryEnd\":\"FR\",\"metadataUUID\":\"032ad16a-ce1b-4ed2-a943-3b3975be9148\",\"transportVehicles\":[{\"id\":0,\"transportMode\":\"ROAD\",\"sequence\":1,\"vehicleID\":null,\"vehicleCountry\":\"FR\",\"journeyStart\":\"2024-01-26T10:54:51+01:00\",\"countryStart\":\"FR\",\"journeyEnd\":\"2024-01-27T10:54:51+01:00\",\"countryEnd\":\"FRANCE\"}],\"isDangerousGoods\":false,\"disabled\":false,\"gateId\":null,\"datasetId\":\"032ad16a-ce1b-4ed2-a943-3b3975be9169\",\"platformId\":\"acme\"}";

        rabbitListenerService.listenReceiveMessage(message);

        assertTrue(memoryAppender.containsFormattedLogMessage(message));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.DEBUG));
    }

    @Test
    void listenReceiveMessageExceptiontest() {
        final String message = "it's gonna explode";

        final Exception exception = assertThrows(TechnicalException.class, () -> {
            rabbitListenerService.listenReceiveMessage(message);
        });

        assertEquals("Error when try to map class eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto with message : it's gonna explode", exception.getMessage());
    }

    @Test()
    void listenSendMessageFailedBuildRequestApRequestDtoTest() {
        final String message = "ok";

        final Exception exception = assertThrows(TechnicalException.class, () -> rabbitListenerService.listenSendMessage(message));

        assertEquals("Error when try to map class eu.efti.eftigate.dto.RabbitRequestDto with message : ok", exception.getMessage());
    }

    @Test
    void listenSendMessageDeadLetterTest() {
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        final String message = "{\"id\":151,\"status\":\"RECEIVED\",\"edeliveryMessageId\":null,\"retry\":0,\"reponseData\":null,\"nextRetryDate\":null,\"createdDate\":[2024,3,5,15,6,52,135892300],\"lastModifiedDate\":null,\"gateIdDest\":\"borduria\",\"control\":{\"id\":102,\"datasetId\":\"12345678-ab12-4ab6-8999-123456789abe\",\"requestId\":\"c5ed0840-bf60-4052-8172-35530d423672\",\"requestType\":\"LOCAL_UIL_SEARCH\",\"status\":\"PENDING\",\"platformId\":\"acme\",\"gateId\":\"borduria\",\"subsetId\":\"full\",\"createdDate\":[2024,3,5,15,6,51,987861600],\"lastModifiedDate\":[2024,3,5,15,6,51,987861600],\"eftiData\":null,\"transportMetaData\":null,\"fromGateId\":null,\"requests\":null,\"authority\":{\"id\":99,\"country\":\"SY\",\"legalContact\":{\"id\":197,\"email\":\"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn.A@63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm\",\"streetName\":\"rue des rossignols\",\"buildingNumber\":\"12\",\"city\":\"Acheville\",\"additionalLine\":null,\"postalCode\":\"62320\"},\"workingContact\":{\"id\":198,\"email\":\"toto@gmail.com\",\"streetName\":\"rue des cafés\",\"buildingNumber\":\"14\",\"city\":\"Lille\",\"additionalLine\":\"osef\",\"postalCode\":\"59000\"},\"isEmergencyService\":null,\"name\":\"aaaa\",\"nationalUniqueIdentifier\":\"aaa\"},\"error\":null,\"metadataResults\":null},\"error\":null}";

        rabbitListenerService.listenSendMessageDeadLetter(message);

        assertTrue(memoryAppender.containsFormattedLogMessage("Receive message for dead queue"));
        assertEquals(1, memoryAppender.countEventsForLogger(LOGGER_NAME, Level.ERROR));
    }
}

