package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.identifiersregistry.service.IdentifiersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EftiAsyncCallsProcessorTest {
    @Mock
    private IdentifiersRequestService identifiersRequestService;
    @Mock
    private IdentifiersService identifiersService;
    @Mock
    private LogManager logManager;
    @Mock
    private GateProperties gateProperties;
    @InjectMocks
    private EftiAsyncCallsProcessor eftiAsyncCallsProcessor;
    private final SearchWithIdentifiersRequestDto identifiersRequestDto = new SearchWithIdentifiersRequestDto();
    private final ControlDto controlDto = new ControlDto();
    ConsignmentDto consignmentDto = ConsignmentDto.builder().build();

    @BeforeEach
    void before() {

        consignmentDto.setGateId("gateId");
        consignmentDto.setDatasetId("datasetId");
        consignmentDto.setPlatformId("platformId");

        this.identifiersRequestDto.setIdentifier("abc123");
        this.identifiersRequestDto.setRegistrationCountryCode("FR");
        this.identifiersRequestDto.setModeCode("ROAD");
    }

    @Test
    void checkLocalRepoTest_whenIdentifiersIsNotPresentInRegistry() {
        //Arrange

        //Act
        eftiAsyncCallsProcessor.checkLocalRepoAsync(identifiersRequestDto, controlDto);

        //Assert
        verify(identifiersService, times(1)).search(identifiersRequestDto);
        verify(identifiersRequestService, times(1)).createRequest(any(ControlDto.class), any(), anyList());
        verify(logManager, times(2)).logRegistryIdentifiers(any(ControlDto.class), any(), any(ComponentType.class), any(ComponentType.class), anyString());
    }
}
