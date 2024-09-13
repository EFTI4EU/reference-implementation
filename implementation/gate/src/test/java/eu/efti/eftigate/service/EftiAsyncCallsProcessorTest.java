package eu.efti.eftigate.service;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.identifiersregistry.service.IdentifiersService;
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

    private final SearchWithIdentifiersRequestDto identifiersRequestDto = new SearchWithIdentifiersRequestDto();

    IdentifiersDto identifiersDto = new IdentifiersDto();

    private final String identifiersUuid = UUID.randomUUID().toString();
    TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
    private final ControlDto controlDto = new ControlDto();


    @BeforeEach
    public void before() {
        final AuthorityDto authorityDto = new AuthorityDto();


        identifiersDto.setIsDangerousGoods(true);
        identifiersDto.setIdentifiersUUID(identifiersUuid);
        identifiersDto.setDisabled(false);
        identifiersDto.setCountryStart("FR");
        identifiersDto.setCountryEnd("FR");
        identifiersDto.setTransportVehicles(Collections.singletonList(transportVehicleDto));


        this.identifiersRequestDto.setVehicleID("abc123");
        this.identifiersRequestDto.setVehicleCountry("FR");
        this.identifiersRequestDto.setAuthority(authorityDto);
        this.identifiersRequestDto.setTransportMode("ROAD");
    }

    @Test
    void checkLocalRepoTest_whenIdentifiersIsNotPresentInRegistry() {
        //Arrange

        //Act
        eftiAsyncCallsProcessor.checkLocalRepoAsync(identifiersRequestDto, controlDto);

        //Assert
        verify(identifiersService, times(1)).search(identifiersRequestDto);
        verify(identifiersRequestService, times(1)).createRequest(any(ControlDto.class), any(), anyList());
        verify(logManager).logLocalRegistryMessage(any(), any());
    }
}
