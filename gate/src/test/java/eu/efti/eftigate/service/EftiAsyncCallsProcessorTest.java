package eu.efti.eftigate.service;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
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
    private IdentifiersRequestService metadataRequestService;
    @Mock
    private IdentifiersService metadataService;
    @Mock
    private LogManager logManager;
    @Mock
    private GateProperties gateProperties;

    @InjectMocks
    private EftiAsyncCallsProcessor eftiAsyncCallsProcessor;

    private final SearchWithIdentifiersRequestDto SearchWithIdentifiersRequestDto = new SearchWithIdentifiersRequestDto();

    IdentifiersDto identifiersDto = new IdentifiersDto();

    private final String metadataUuid = UUID.randomUUID().toString();
    TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
    private final ControlDto controlDto = new ControlDto();


    @BeforeEach
    public void before() {
        final AuthorityDto authorityDto = new AuthorityDto();

        identifiersDto.setIsDangerousGoods(true);
        identifiersDto.setIdentifiersUUID(metadataUuid);
        identifiersDto.setDisabled(false);
        identifiersDto.setCountryStart("FR");
        identifiersDto.setCountryEnd("FR");
        identifiersDto.setTransportVehicles(Collections.singletonList(transportVehicleDto));

        this.SearchWithIdentifiersRequestDto.setVehicleID("abc123");
        this.SearchWithIdentifiersRequestDto.setVehicleCountry("FR");
        this.SearchWithIdentifiersRequestDto.setAuthority(authorityDto);
        this.SearchWithIdentifiersRequestDto.setTransportMode("ROAD");
    }

    @Test
    void checkLocalRepoTest_whenMetadataIsNotPresentInRegistry() {
        //Arrange

        //Act
        eftiAsyncCallsProcessor.checkLocalRepoAsync(SearchWithIdentifiersRequestDto, controlDto);

        //Assert
        verify(metadataService, times(1)).search(SearchWithIdentifiersRequestDto);
        verify(metadataRequestService, times(1)).createRequest(any(ControlDto.class), any(), anyList());
    }
}
