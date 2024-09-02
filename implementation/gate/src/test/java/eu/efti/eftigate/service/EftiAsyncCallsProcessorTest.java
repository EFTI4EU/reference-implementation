package eu.efti.eftigate.service;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.commons.dto.ConsignmentIdentifiersRequestDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.identifierregistry.service.IdentifiersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

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

    private final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto = new ConsignmentIdentifiersRequestDto();

    ConsignmentIdentifiersDTO consignmentIdentifiersDTO = new ConsignmentIdentifiersDTO();

    private final String consignmentUUID = UUID.randomUUID().toString();
    TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
    private final ControlDto controlDto = new ControlDto();


    @BeforeEach
    public void before() {
        final AuthorityDto authorityDto = new AuthorityDto();


        consignmentIdentifiersDTO.setDangerousGoods(true);
        consignmentIdentifiersDTO.setConsignmentUUID(consignmentUUID);
        consignmentIdentifiersDTO.setDisabled(false);
        consignmentIdentifiersDTO.setCountryStart("FR");
        consignmentIdentifiersDTO.setCountryEnd("FR");
        consignmentIdentifiersDTO.setTransportVehicles(Collections.singletonList(transportVehicleDto));


        this.consignmentIdentifiersRequestDto.setVehicleID("abc123");
        this.consignmentIdentifiersRequestDto.setVehicleCountry("FR");
        this.consignmentIdentifiersRequestDto.setAuthority(authorityDto);
        this.consignmentIdentifiersRequestDto.setTransportMode("ROAD");
    }

    @Test
    void checkLocalRepoTest_whenIdentifiersAreNotPresentInRegistry() {
        //Arrange

        //Act
        eftiAsyncCallsProcessor.checkLocalRepoAsync(consignmentIdentifiersRequestDto, controlDto);

        //Assert
        verify(identifiersService, times(1)).search(consignmentIdentifiersRequestDto);
        verify(identifiersRequestService, times(1)).createRequest(any(ControlDto.class), any(), anyList());
        verify(logManager).logLocalRegistryMessage(any(), any());
    }
}
