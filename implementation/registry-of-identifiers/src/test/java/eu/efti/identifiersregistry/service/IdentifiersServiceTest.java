package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.identifiersregistry.entity.Identifiers;
import eu.efti.identifiersregistry.exception.InvalidIdentifiersException;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
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

    private IdentifiersDto identifiersDto;
    private Identifiers identifiers;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        service = new IdentifiersService(repository, mapperUtils, auditRegistryLogService, serializeUtils);

        ReflectionTestUtils.setField(service, "gateOwner", "http://efti.gate.borduria.eu");
        ReflectionTestUtils.setField(service, "gateCountry", "BO");

        identifiersDto = IdentifiersDto.builder()
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL)
                .transportVehicles(List.of(TransportVehicleDto.builder()
                                .vehicleId("abc123").countryStart("FR").countryEnd("toto").build(),
                        TransportVehicleDto.builder()
                                .vehicleId("abc124").countryStart("osef").countryEnd("IT").build())).build();

        identifiers = Identifiers.builder()
                .eFTIGateUrl(GATE_URL)
                .eFTIDataUuid(DATA_UUID)
                .eFTIPlatformUrl(PLATFORM_URL).build();
    }

    @Test
    void shouldCreateIdentifiers() {
        when(repository.save(any())).thenReturn(identifiers);
        final ArgumentCaptor<Identifiers> argumentCaptor = ArgumentCaptor.forClass(Identifiers.class);

        service.createOrUpdate(identifiersDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldCreateIdentifiersAndIgnoreWrongsFields() {
        when(repository.save(any())).thenReturn(identifiers);
        final ArgumentCaptor<Identifiers> argumentCaptor = ArgumentCaptor.forClass(Identifiers.class);

        service.createOrUpdate(identifiersDto);

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
        when(repository.save(any())).thenReturn(identifiers);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());
        final ArgumentCaptor<Identifiers> argumentCaptor = ArgumentCaptor.forClass(Identifiers.class);

        service.createOrUpdate(identifiersDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldExistByUil() {
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.of(Identifiers.builder().build()));

        assertTrue(service.existByUIL(DATA_UUID, GATE_URL, PLATFORM_URL));
    }

    @Test
    void shouldNotExistByUil() {
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());

        assertFalse(service.existByUIL(DATA_UUID, GATE_URL, PLATFORM_URL));
    }

    @Test
    void shouldUpdateIfUILFound() {
        when(repository.save(any())).thenReturn(identifiers);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.of(Identifiers.builder().build()));
        final ArgumentCaptor<Identifiers> argumentCaptor = ArgumentCaptor.forClass(Identifiers.class);

        service.createOrUpdate(identifiersDto);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getEFTIDataUuid());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getEFTIPlatformUrl());
        assertEquals(GATE_URL, argumentCaptor.getValue().getEFTIGateUrl());
    }

    @Test
    void shouldThrowIfIdentifiersNotValid() {
        identifiersDto.setEFTIDataUuid("wrong");

        assertThrows(InvalidIdentifiersException.class, () -> service.createOrUpdate(identifiersDto));
    }

    @Test
    void shouldDisable() {
        when(repository.save(any())).thenReturn(identifiers);
        final ArgumentCaptor<Identifiers> captor = ArgumentCaptor.forClass(Identifiers.class);
        service.disable(identifiersDto);

        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue());
        assertTrue(captor.getValue().isDisabled());
    }

    @Test
    void shouldSearch() {
        final SearchWithIdentifiersRequestDto identifiersRequestDto = SearchWithIdentifiersRequestDto.builder().build();
        service.search(identifiersRequestDto);
        verify(repository).searchByCriteria(identifiersRequestDto);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }
}
