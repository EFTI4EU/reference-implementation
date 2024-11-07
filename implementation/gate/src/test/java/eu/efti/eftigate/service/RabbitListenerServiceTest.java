package eu.efti.eftigate.service;

import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.service.request.UilRequestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static eu.efti.eftigate.EftiTestUtils.testFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RabbitListenerServiceTest extends BaseServiceTest {
    @Mock
    private RequestSendingService requestSendingService;
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private ApIncomingService apIncomingService;
    @Mock
    private LogManager logManager;


    private static final String URL = "url";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";

    private RabbitListenerService rabbitListenerService;

    @Override
    @BeforeEach
    public void before() {

        final GateProperties gateProperties = GateProperties.builder()
                .owner("http://france.lol")
                .ap(GateProperties.ApConfig.builder()
                        .url(URL)
                        .password(PASSWORD)
                        .username(USERNAME).build()).build();

        rabbitListenerService = new RabbitListenerService(gateProperties, serializeUtils, requestSendingService,
                requestServiceFactory, apIncomingService, mapperUtils, logManager);
    }


    @Test
    void listenMessageReceiveDeadQueueTest() {
        final String message = "oki";

        rabbitListenerService.listenMessageReceiveDeadQueue(message);
    }

    @Test
    void listenReceiveMessageTest() {
        final String message = "{\"id\":0,\"journeyStart\":\"2024-01-26T10:54:51+01:00\",\"countryStart\":\"FR\",\"journeyEnd\":\"2024-01-27T10:54:51+01:00\",\"countryEnd\":\"FR\",\"metadataUUID\":\"032ad16a-ce1b-4ed2-a943-3b3975be9148\",\"transportVehicles\":[{\"id\":0,\"transportMode\":\"ROAD\",\"sequence\":1,\"vehicleID\":null,\"vehicleCountry\":\"FR\",\"journeyStart\":\"2024-01-26T10:54:51+01:00\",\"countryStart\":\"FR\",\"journeyEnd\":\"2024-01-27T10:54:51+01:00\",\"countryEnd\":\"FRANCE\"}],\"isDangerousGoods\":false,\"disabled\":false,\"gateId\":null,\"eFTIDataUuid\":\"032ad16a-ce1b-4ed2-a943-3b3975be9169\",\"platformId\":\"acme\"}";

        rabbitListenerService.listenReceiveMessage(message);
    }

    @Test
    void listenReceiveMessageExceptiontest() {
        final String message = "ça va pétteeeeer";

        final Exception exception = assertThrows(TechnicalException.class, () -> {
            rabbitListenerService.listenReceiveMessage(message);
        });

        assertEquals("Error when try to map class eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto with message : ça va pétteeeeer", exception.getMessage());
    }

    @Test
    void listenSendMessageTest() {
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        final String requestJson = testFile("/json/localuilrequest.json");

        rabbitListenerService.listenSendMessage(StringUtils.deleteWhitespace(requestJson));

        verify(logManager).logSentMessage(any(), any(), anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test()
    void listenSendMessageFailedBuildRequestApRequestDtoTest() {
        final String message = "oki";

        final Exception exception = assertThrows(TechnicalException.class, () -> rabbitListenerService.listenSendMessage(message));

        assertEquals("Error when try to map class eu.efti.eftigate.dto.RabbitRequestDto with message : oki", exception.getMessage());
    }

    @Test
    void listenSendMessageFailedSendDomibusTest() {
        final String message = "{\"id\":151,\"status\":\"RECEIVED\",\"edeliveryMessageId\":null,\"retry\":0,\"requestType\":\"UIL\",\"reponseData\":null,\"nextRetryDate\":null,\"createdDate\":[2024,3,5,15,6,52,135892300],\"lastModifiedDate\":null,\"gateIdDest\":\"borduria\",\"control\":{\"id\":102,\"eftiDataUuid\":\"12345678-ab12-4ab6-8999-123456789abe\",\"requestId\":\"c5ed0840-bf60-4052-8172-35530d423672\",\"requestType\":\"LOCAL_UIL_SEARCH\",\"status\":\"PENDING\",\"platformId\":\"acme\",\"gateId\":\"borduria\",\"subseId\":\"full\",\"createdDate\":[2024,3,5,15,6,51,987861600],\"lastModifiedDate\":[2024,3,5,15,6,51,987861600],\"eftiData\":null,\"transportMetaData\":null,\"fromGateId\":null,\"requests\":null,\"authority\":{\"id\":99,\"country\":\"SY\",\"legalContact\":{\"id\":197,\"email\":\"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn.A@63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm\",\"streetName\":\"rue des rossignols\",\"buildingNumber\":\"12\",\"city\":\"Acheville\",\"additionalLine\":null,\"postalCode\":\"62320\"},\"workingContact\":{\"id\":198,\"email\":\"toto@gmail.com\",\"streetName\":\"rue des cafés\",\"buildingNumber\":\"14\",\"city\":\"Lille\",\"additionalLine\":\"osef\",\"postalCode\":\"59000\"},\"isEmergencyService\":null,\"name\":\"aaaa\",\"nationalUniqueIdentifier\":\"aaa\"},\"error\":null,\"metadataResults\":null},\"error\":null}";
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(requestSendingService.sendRequest(any())).thenThrow(SendRequestException.class);
        final Exception exception = assertThrows(TechnicalException.class, () -> rabbitListenerService.listenSendMessage(message));
        verify(logManager).logSentMessage(any(), any(), anyString(), anyBoolean(), anyBoolean(), any());
        assertEquals("Error when try to send message to domibus", exception.getMessage());
    }

    @Test
    void listenSendMessageDeadLetterTest() {
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        final String message = "{\"id\":151,\"status\":\"RECEIVED\",\"edeliveryMessageId\":null,\"retry\":0,\"reponseData\":null,\"nextRetryDate\":null,\"createdDate\":[2024,3,5,15,6,52,135892300],\"lastModifiedDate\":null,\"gateIdDest\":\"borduria\",\"control\":{\"id\":102,\"eftiDataUuid\":\"12345678-ab12-4ab6-8999-123456789abe\",\"requestId\":\"c5ed0840-bf60-4052-8172-35530d423672\",\"requestType\":\"LOCAL_UIL_SEARCH\",\"status\":\"PENDING\",\"platformId\":\"acme\",\"gateId\":\"borduria\",\"subsetId\":\"full\",\"createdDate\":[2024,3,5,15,6,51,987861600],\"lastModifiedDate\":[2024,3,5,15,6,51,987861600],\"eftiData\":null,\"transportMetaData\":null,\"fromGateId\":null,\"requests\":null,\"authority\":{\"id\":99,\"country\":\"SY\",\"legalContact\":{\"id\":197,\"email\":\"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn.A@63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm\",\"streetName\":\"rue des rossignols\",\"buildingNumber\":\"12\",\"city\":\"Acheville\",\"additionalLine\":null,\"postalCode\":\"62320\"},\"workingContact\":{\"id\":198,\"email\":\"toto@gmail.com\",\"streetName\":\"rue des cafés\",\"buildingNumber\":\"14\",\"city\":\"Lille\",\"additionalLine\":\"osef\",\"postalCode\":\"59000\"},\"isEmergencyService\":null,\"name\":\"aaaa\",\"nationalUniqueIdentifier\":\"aaa\"},\"error\":null,\"metadataResults\":null},\"error\":null}";

        rabbitListenerService.listenSendMessageDeadLetter(message);
    }
}

