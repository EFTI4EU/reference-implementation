package eu.efti.identifierregistry.service;

import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.commons.dto.ConsignmentIdentifiersRequestDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.identifierregistry.entity.Consignment;
import eu.efti.identifierregistry.exception.InvalidIdentifiersException;
import eu.efti.identifierregistry.repository.IdentifiersRepository;
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

class IdentifiersServiceTest extends AbstractServiceTest {

    public static final String GATE_URL = "http://efti.gate.borduria.eu";
    public static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    public static final String PLATFORM_URL = "http://efti.platform.truc.eu";
    AutoCloseable openMocks;

    private IdentifiersService service;
    @Mock
    private IdentifiersRepository repository;

    private ConsignmentIdentifiersDTO consignmentIdentifiersDTO;
    private Consignment consignment;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        service = new IdentifiersService(repository, mapperUtils, auditRegistryLogService, serializeUtils);

        ReflectionTestUtils.setField(service, "gateOwner", "http://efti.gate.borduria.eu");
        ReflectionTestUtils.setField(service, "gateCountry", "BO");

        consignmentIdentifiersDTO = ConsignmentIdentifiersDTO.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicleDto.builder()
                                .vehicleId("abc123").countryStart("FR").countryEnd("toto").build(),
                        TransportVehicleDto.builder()
                                .vehicleId("abc124").countryStart("osef").countryEnd("IT").build())).build();

        consignment = Consignment.builder()
                .eFTIGateUrl(GATE_URL)
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL).build();
    }

    @Test
    void shouldCreateMetadata() {
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(consignmentIdentifiersDTO);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldCreateMetadataAndIgnoreWrongsFields() {
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(consignmentIdentifiersDTO);

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
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(consignmentIdentifiersDTO);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldExistByUil() {
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.of(Consignment.builder().build()));

        assertTrue(service.existByUIL(DATA_UUID, GATE_URL, PLATFORM_URL));
    }

    @Test
    void shouldNotExistByUil() {
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());

        assertFalse(service.existByUIL(DATA_UUID, GATE_URL, PLATFORM_URL));
    }

    @Test
    void shouldUpdateIfUILFound() {
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.of(Consignment.builder().build()));
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(consignmentIdentifiersDTO);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldThrowIfMetadataNotValid() {
        consignmentIdentifiersDTO.setEFTIDataUuid("wrong");

        assertThrows(InvalidIdentifiersException.class, () -> service.createOrUpdate(consignmentIdentifiersDTO));
    }

    @Test
    void shouldDisable() {
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> captor = ArgumentCaptor.forClass(Consignment.class);
        service.disable(consignmentIdentifiersDTO);

        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue());
        assertTrue(captor.getValue().isDisabled());
    }

    @Test
    void shouldSearch() {
        final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto = ConsignmentIdentifiersRequestDto.builder().build();
        service.search(consignmentIdentifiersRequestDto);
        verify(repository).searchByCriteria(consignmentIdentifiersRequestDto);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
