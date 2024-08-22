package com.ingroupe.efti.metadataregistry.service;

import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.TransportVehicleDto;
import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.metadataregistry.entity.MetadataEntity;
import com.ingroupe.efti.metadataregistry.exception.InvalidMetadataException;
import com.ingroupe.efti.metadataregistry.repository.MetadataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertNull;

class MetadataServiceTest extends AbstractServiceTest {

    public static final String GATE_URL = "http://efti.gate.borduria.eu";
    public static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    public static final String PLATFORM_URL = "http://efti.platform.truc.eu";
    AutoCloseable openMocks;

    private MetadataService service;
    @Mock
    private MetadataRepository repository;

    private MetadataDto metadataDto;
    private MetadataEntity metadata;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        service = new MetadataService(repository, mapperUtils, auditRegistryLogService, serializeUtils);

        ReflectionTestUtils.setField(service, "gateOwner", "http://efti.gate.borduria.eu");
        ReflectionTestUtils.setField(service, "gateCountry", "BO");

        metadataDto = MetadataDto.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicleDto.builder()
                                .vehicleID("abc123").countryStart("FR").countryEnd("toto").build(),
                        TransportVehicleDto.builder()
                                .vehicleID("abc124").countryStart("osef").countryEnd("IT").build())).build();

        metadata = MetadataEntity.builder()
                .eFTIGateUrl(GATE_URL)
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL).build();
    }

    @Test
    void shouldCreateMetadata() {
        when(repository.save(any())).thenReturn(metadata);
        final ArgumentCaptor<MetadataEntity> argumentCaptor = ArgumentCaptor.forClass(MetadataEntity.class);

        service.createOrUpdate(metadataDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldCreateMetadataAndIgnoreWrongsFields() {
        when(repository.save(any())).thenReturn(metadata);
        final ArgumentCaptor<MetadataEntity> argumentCaptor = ArgumentCaptor.forClass(MetadataEntity.class);

        service.createOrUpdate(metadataDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
        assertEquals(CountryIndicator.FR, argumentCaptor.getValue().getTransportVehicles().get(0).getCountryStart());
        assertNull(null, argumentCaptor.getValue().getTransportVehicles().get(0).getCountryEnd());
        assertEquals(CountryIndicator.IT, argumentCaptor.getValue().getTransportVehicles().get(1).getCountryEnd());
        assertNull(null, argumentCaptor.getValue().getTransportVehicles().get(1).getCountryStart());
    }

    @Test
    void shouldCreateIfUilNotFound() {
        when(repository.save(any())).thenReturn(metadata);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());
        final ArgumentCaptor<MetadataEntity> argumentCaptor = ArgumentCaptor.forClass(MetadataEntity.class);

        service.createOrUpdate(metadataDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldExistByUil() {
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.of(MetadataEntity.builder().build()));

        assertTrue(service.existByUIL(DATA_UUID, GATE_URL, PLATFORM_URL));
    }

    @Test
    void shouldNotExistByUil() {
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());

        assertFalse(service.existByUIL(DATA_UUID, GATE_URL, PLATFORM_URL));
    }

    @Test
    void shouldUpdateIfUILFound() {
        when(repository.save(any())).thenReturn(metadata);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.of(MetadataEntity.builder().build()));
        final ArgumentCaptor<MetadataEntity> argumentCaptor = ArgumentCaptor.forClass(MetadataEntity.class);

        service.createOrUpdate(metadataDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldThrowIfMetadataNotValid() {
        metadataDto.setEFTIDataUuid("wrong");

        assertThrows(InvalidMetadataException.class, () -> service.createOrUpdate(metadataDto));
    }

    @Test
    void shouldDisable() {
        when(repository.save(any())).thenReturn(metadata);
        final ArgumentCaptor<MetadataEntity> captor = ArgumentCaptor.forClass(MetadataEntity.class);
        service.disable(metadataDto);

        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue());
        assertTrue(captor.getValue().isDisabled());
    }

    @Test
    void shouldSearch() {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder().build();
        service.search(metadataRequestDto);
        verify(repository).searchByCriteria(metadataRequestDto);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
