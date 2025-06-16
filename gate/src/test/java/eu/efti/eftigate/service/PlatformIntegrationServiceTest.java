package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.enums.RequestType;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.feign.PlatformClient;
import eu.efti.eftigate.service.gate.EftiPlatformIdResolver;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.UilRequestService;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.ArrayList;

import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.LOCAL_UIL_SEARCH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformIntegrationServiceTest extends AbstractServiceTest {

    @Mock
    private PlatformClient platformClient;

    @Mock
    private UilRequestService uilRequestService;

    @Mock
    private NotesRequestService notesRequestService;

    @Mock
    private EftiPlatformIdResolver eftiPlatformIdResolver;

    private PlatformIntegrationService platformIntegrationService;

    @BeforeEach
    void before() {
        platformIntegrationService = new PlatformIntegrationService(platformClient, uilRequestService, notesRequestService, serializeUtils, eftiPlatformIdResolver);
    }

    @Test
    void testHandleUilQuery() {
        //Arrange
        ControlDto controlDto = new ControlDto();
        controlDto.setDatasetId("12345678-ab12-4ab6-8999-123456789abc");
        controlDto.setRequestType(EXTERNAL_ASK_IDENTIFIERS_SEARCH);
        controlDto.setRequestId("requestId");
        controlDto.setSubsetIds(new ArrayList<>());
        controlDto.setPlatformId("platformId");

        RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setRequestType(RequestType.UIL);

        String responseBody = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <consignment xmlns="http://efti.eu/v1/consignment/common">
                	<applicableServiceCharge>
                		<appliedAmount currencyId="ZWL">7.02</appliedAmount>
                		<calculationBasisCode>swaq</calculationBasisCode>
                		<id>aytd</id>
                		<payingPartyRoleCode>vkdc</payingPartyRoleCode>
                		<paymentArrangementCode>hnpe</paymentArrangementCode>
                	</applicableServiceCharge>
                </consignment>
                """;

        when(platformClient.sendUilQuery(URI.create("dummy-uri"), "12345678-ab12-4ab6-8999-123456789abc", new ArrayList<>()))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));
        when(eftiPlatformIdResolver.getPlatformUrlByPlatformId("platformId")).thenReturn("dummy-uri");

        //Act
        platformIntegrationService.handle(rabbitRequestDto);

        //Assert
        verify(uilRequestService, times(1)).manageRestRequestInProgress("requestId");
        verify(platformClient, times(1)).sendUilQuery(URI.create("dummy-uri"), controlDto.getDatasetId(), controlDto.getSubsetIds());
        verify(uilRequestService, times(1)).manageRestResponseReceivedFromPlatform(any(), any(SupplyChainConsignment.class));
        verify(notesRequestService, never()).manageRestRequestInProgress(anyString());
    }

    @Test
    void testHandlePostFollowUpQuery() {
        //Arrange
        ControlDto controlDto = new ControlDto();
        controlDto.setDatasetId("12345678-ab12-4ab6-8999-123456789abc");
        controlDto.setRequestType(LOCAL_UIL_SEARCH);
        controlDto.setRequestId("requestId");
        controlDto.setSubsetIds(new ArrayList<>());
        controlDto.setPlatformId("platformId");

        RabbitRequestDto rabbitRequestDto = new RabbitRequestDto();
        rabbitRequestDto.setControl(controlDto);
        rabbitRequestDto.setNote("Suspicious");
        rabbitRequestDto.setRequestType(RequestType.NOTE);

        when(platformClient.postConsignmentFollowUp(URI.create("baseUri"), "12345678-ab12-4ab6-8999-123456789abc", "Suspicious"))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(eftiPlatformIdResolver.getPlatformUrlByPlatformId("platformId")).thenReturn("baseUri");


        //Act
        platformIntegrationService.handle(rabbitRequestDto);

        //Assert
        verify(uilRequestService, never()).manageRestRequestInProgress("requestId");
        verify(platformClient, times(1)).postConsignmentFollowUp(URI.create("baseUri"), controlDto.getDatasetId(), rabbitRequestDto.getNote());
        verify(notesRequestService, times(1)).manageRestRequestInProgress(anyString());
        verify(notesRequestService, times(1)).manageRestRequestDone(anyString());
    }

}
