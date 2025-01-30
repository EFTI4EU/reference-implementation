package eu.efti.platformgatesimulator.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.service.AbstractTest;
import eu.efti.platformgatesimulator.service.IdentifierService;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.IdentifierQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(GateProperties.class)
class IdentifierServiceTest extends AbstractTest {

    AutoCloseable openMocks;

    @Mock
    private RequestSendingService requestSendingService;

    private IdentifierService identifierService;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .minSleep(1000)
                .maxSleep(2000)
                .cdaPath("./cda/")
                .ap(GateProperties.ApConfig.builder()
                        .url("url")
                        .password("password")
                        .username("username").build()).build();
        identifierService = new IdentifierService(requestSendingService, gateProperties, serializeUtils);
        setField(identifierService, "average", 10);
        setField(identifierService, "standardDeviation", 5);
        setField(identifierService, "isActiveForIdentifierRequestTimer", false);
        setField(identifierService, "isTimerActiveForIdentifierResponse", false);
        setField(identifierService, "descriptionIdentifierResponse", "description");
        setField(identifierService, "statusIdentifierResponse", "200");
        setField(identifierService, "descriptionIdentifierBadResponse", "bad gateway");
        setField(identifierService, "statusIdentifierBadResponse", "404");
        setField(identifierService, "badRequestPercentage", 0.5f);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void sendResponseUilGoodRequestTest() {
        setField(identifierService, "badRequestPercentage", 0f);
        when(requestSendingService.sendRequest(any())).thenReturn(null);

        identifierService.sendResponseUil("requestId", new SupplyChainConsignment());

        verify(requestSendingService, Mockito.times(1)).sendRequest(any());
    }

    @Test
    void sendResponseUilBadGoodRequestTest() {
        setField(identifierService, "badRequestPercentage", 1f);
        when(requestSendingService.sendRequest(any())).thenReturn(null);

        identifierService.sendResponseUil("requestId", new SupplyChainConsignment());

        verify(requestSendingService, Mockito.times(1)).sendRequest(any());
    }

    @Test
    void sendRequestUilTest() {
        final UilDto uilDto = UilDto.builder()
                .subsetIds(List.of("subsetId"))
                .datasetId("datasetId")
                .gateId("gateId")
                .platformId("platformId").build();
        setField(identifierService, "isTimerActiveForIdentifierResponse", true);

        when(requestSendingService.sendRequest(any())).thenReturn(null);

        identifierService.sendRequestUil(uilDto);

        verify(requestSendingService, times(1)).sendRequest(any());
    }

    @Test
    void sendIdentifierRequestTest() {
        final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto = new SearchWithIdentifiersRequestDto();
        setField(identifierService, "isActiveForIdentifierRequestTimer", true);
        when(requestSendingService.sendRequest(any())).thenReturn(null);

        identifierService.sendIdentifierRequest(searchWithIdentifiersRequestDto);

        verify(requestSendingService, times(1)).sendRequest(any());
    }

    @Test
    void sendResponseIdentifierGoodResponseTest() {
        final IdentifierQuery identifierQuery = new IdentifierQuery();
        final NotificationDto notificationDto = new NotificationDto();
        identifierQuery.setRequestId("requestId");
        notificationDto.setContent(NotificationContentDto.builder().fromPartyId("fromParty").body("body").conversationId("conversationId")
                .contentType("contentType").messageId("messageId").build());
        setField(identifierService, "badRequestPercentage", 0f);

        when(requestSendingService.sendRequest(any())).thenReturn(null);

        identifierService.sendResponseIdentifier(identifierQuery, notificationDto);

        verify(requestSendingService, times(1)).sendRequest(any());
    }

    @Test
    void sendResponseIdentifierBadResponseTest() {
        final IdentifierQuery identifierQuery = new IdentifierQuery();
        final NotificationDto notificationDto = new NotificationDto();
        identifierQuery.setRequestId("requestId");
        notificationDto.setContent(NotificationContentDto.builder().fromPartyId("fromParty").body("body").conversationId("conversationId")
                .contentType("contentType").messageId("messageId").build());
        setField(identifierService, "badRequestPercentage", 1f);

        when(requestSendingService.sendRequest(any())).thenReturn(null);

        identifierService.sendResponseIdentifier(identifierQuery, notificationDto);

        verify(requestSendingService, times(1)).sendRequest(any());
    }
}
