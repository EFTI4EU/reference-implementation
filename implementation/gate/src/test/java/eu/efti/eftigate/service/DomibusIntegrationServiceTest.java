package eu.efti.eftigate.service;

import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.generator.id.MessageIdGenerator;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.service.request.UilRequestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
class DomibusIntegrationServiceTest extends BaseServiceTest {
    @Mock
    private RequestSendingService requestSendingService;
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private UilRequestService uilRequestService;
    @Mock
    private LogManager logManager;
    @Mock
    private MessageIdGenerator messageIdGenerator;

    private static final String URL = "url";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";

    private static final GateProperties gateProperties = GateProperties.builder()
            .owner("http://france.lol")
            .ap(GateProperties.ApConfig.builder()
                    .url(URL)
                    .password(PASSWORD)
                    .username(USERNAME).build()).build();

    @Test
    void listenSendMessageUilTest() {
        when(requestServiceFactory.getRequestServiceByRequestType(any(String.class))).thenReturn(uilRequestService);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        final String requestJson = testFile("/json/localuilrequest.json");

        var rabbitRequestDto = serializeUtils.mapJsonStringToClass(StringUtils.deleteWhitespace(requestJson), RabbitRequestDto.class);

        var domibusIntegrationService = new DomibusIntegrationService(gateProperties, serializeUtils, requestSendingService, requestServiceFactory, mapperUtils, logManager, messageIdGenerator);
        domibusIntegrationService.trySendDomibus(rabbitRequestDto, rabbitRequestDto.getControl().getRequestType(), rabbitRequestDto.getGateIdDest());

        verify(logManager).logSentMessage(any(), any(), anyString(), any(), any(), anyBoolean(), any());
    }

    @Test
    void listenSendMessageFailedSendDomibusTest() {
        final String message = "{\"id\":151,\"status\":\"RECEIVED\",\"edeliveryMessageId\":null,\"retry\":0,\"requestType\":\"UIL\",\"reponseData\":null,\"nextRetryDate\":null,\"createdDate\":[2024,3,5,15,6,52,135892300],\"lastModifiedDate\":null,\"gateIdDest\":\"borduria\",\"control\":{\"id\":102,\"datasetId\":\"12345678-ab12-4ab6-8999-123456789abe\",\"requestId\":\"c5ed0840-bf60-4052-8172-35530d423672\",\"requestType\":\"LOCAL_UIL_SEARCH\",\"status\":\"PENDING\",\"platformId\":\"acme\",\"gateId\":\"borduria\",\"subseId\":\"full\",\"createdDate\":[2024,3,5,15,6,51,987861600],\"lastModifiedDate\":[2024,3,5,15,6,51,987861600],\"eftiData\":null,\"transportMetaData\":null,\"fromGateId\":null,\"requests\":null,\"authority\":{\"id\":99,\"country\":\"SY\",\"legalContact\":{\"id\":197,\"email\":\"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn.A@63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.63ccccccccccccccccccccccccccccccccccccccccccccccccccccccccgmail.commmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm\",\"streetName\":\"rue des rossignols\",\"buildingNumber\":\"12\",\"city\":\"Acheville\",\"additionalLine\":null,\"postalCode\":\"62320\"},\"workingContact\":{\"id\":198,\"email\":\"toto@gmail.com\",\"streetName\":\"rue des cafés\",\"buildingNumber\":\"14\",\"city\":\"Lille\",\"additionalLine\":\"osef\",\"postalCode\":\"59000\"},\"isEmergencyService\":null,\"name\":\"aaaa\",\"nationalUniqueIdentifier\":\"aaa\"},\"error\":null,\"metadataResults\":null},\"error\":null}";
        when(requestServiceFactory.getRequestServiceByRequestType(any(String.class))).thenReturn(uilRequestService);
        when(requestSendingService.sendRequest(any())).thenThrow(SendRequestException.class);
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);

        var rabbitRequestDto = serializeUtils.mapJsonStringToClass(StringUtils.deleteWhitespace(message), RabbitRequestDto.class);
        var domibusIntegrationService = new DomibusIntegrationService(gateProperties, serializeUtils, requestSendingService, requestServiceFactory, mapperUtils, logManager, messageIdGenerator);
        final Exception exception = assertThrows(TechnicalException.class, () -> domibusIntegrationService.trySendDomibus(rabbitRequestDto, rabbitRequestDto.getControl().getRequestType(), rabbitRequestDto.getGateIdDest()));

        verify(logManager).logSentMessage(any(), any(), anyString(), any(), any(), anyBoolean(), any());
        assertEquals("Error when try to send message to domibus", exception.getMessage());
    }
}

