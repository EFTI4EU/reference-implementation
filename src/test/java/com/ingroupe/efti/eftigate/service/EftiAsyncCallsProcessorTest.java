package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.dto.AuthorityDto;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.TransportVehicleDto;
import com.ingroupe.efti.eftigate.config.GateProperties;
import com.ingroupe.efti.eftigate.service.request.MetadataRequestService;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
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
    private MetadataRequestService metadataRequestService;
    @Mock
    private MetadataService metadataService;
    @Mock
    private LogManager logManager;
    @Mock
    private GateProperties gateProperties;

    @InjectMocks
    private EftiAsyncCallsProcessor eftiAsyncCallsProcessor;

    private final MetadataRequestDto metadataRequestDto = new MetadataRequestDto();

    MetadataDto metadataDto= new MetadataDto();

    private final String metadataUuid = UUID.randomUUID().toString();
    TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
    private final ControlDto controlDto = new ControlDto();


    @BeforeEach
    public void before() {
        final AuthorityDto authorityDto = new AuthorityDto();


        metadataDto.setIsDangerousGoods(true);
        metadataDto.setMetadataUUID(metadataUuid);
        metadataDto.setDisabled(false);
        metadataDto.setCountryStart("FR");
        metadataDto.setCountryEnd("FR");
        metadataDto.setTransportVehicles(Collections.singletonList(transportVehicleDto));


        this.metadataRequestDto.setVehicleID("abc123");
        this.metadataRequestDto.setVehicleCountry("FR");
        this.metadataRequestDto.setAuthority(authorityDto);
        this.metadataRequestDto.setTransportMode("ROAD");
    }

    @Test
    void checkLocalRepoTest_whenMetadataIsNotPresentInRegistry() {
        //Arrange

        //Act
        eftiAsyncCallsProcessor.checkLocalRepoAsync(metadataRequestDto, controlDto);

        //Assert
        verify(metadataService, times(1)).search(metadataRequestDto);
        verify(metadataRequestService, times(1)).createRequest(any(ControlDto.class), any(), anyList());
        verify(logManager).logLocalRegistryMessage(any(), any());
    }
}
