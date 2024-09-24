package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.consignment.identifier.TransportEvent;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import eu.efti.v1.types.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentifiersServiceTest extends AbstractServiceTest {

    public static final String GATE_URL = "http://efti.gate.borduria.eu";
    public static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    public static final String PLATFORM_URL = "http://efti.platform.truc.eu";
    AutoCloseable openMocks;

    private IdentifiersService service;
    @Mock
    private IdentifiersRepository repository;

    private SaveIdentifiersRequestWrapper saveIdentifiersRequestWrapper;
    private Consignment consignment;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        service = new IdentifiersService(repository, mapperUtils, auditRegistryLogService, serializeUtils);

        ReflectionTestUtils.setField(service, "gateOwner", "http://efti.gate.borduria.eu");
        ReflectionTestUtils.setField(service, "gateCountry", "BO");

        SaveIdentifiersRequest identifiersRequest = defaultSaveIdentifiersRequest();
        saveIdentifiersRequestWrapper = new SaveIdentifiersRequestWrapper(PLATFORM_URL, identifiersRequest);


        consignment = Consignment.builder()
                .gateId(GATE_URL)
                .datasetId(DATA_UUID)
                .platformId(PLATFORM_URL).build();
    }

    private static SaveIdentifiersRequest defaultSaveIdentifiersRequest() {
        var occurenceDateTime = new DateTime();
        occurenceDateTime.setValue("202107111200+0100");
        occurenceDateTime.setFormatId("205");

        TransportEvent transportEvent = new TransportEvent();
        transportEvent.setActualOccurrenceDateTime(occurenceDateTime);

        var acceptanceDate = new DateTime();
        acceptanceDate.setValue("202107111200+0100");
        acceptanceDate.setFormatId("205");

        SupplyChainConsignment sourceConsignment = new SupplyChainConsignment();
        sourceConsignment.setDeliveryEvent(transportEvent);
        sourceConsignment.setCarrierAcceptanceDateTime(acceptanceDate);

        SaveIdentifiersRequest identifiersRequest = new SaveIdentifiersRequest();
        identifiersRequest.setDatasetId(DATA_UUID);
        identifiersRequest.setConsignment(sourceConsignment);
        return identifiersRequest;
    }

    @Test
    void shouldCreateIdentifiers() {
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_URL, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldCreateIdentifiersAndIgnoreWrongsFields() {
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_URL, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldCreateIfUilNotFound() {
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findByUil(GATE_URL, DATA_UUID, PLATFORM_URL)).thenReturn(Optional.empty());
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_URL, argumentCaptor.getValue().getGateId());
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

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        verify(repository).findByUil(GATE_URL, DATA_UUID, PLATFORM_URL);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_URL, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_URL, argumentCaptor.getValue().getGateId());
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
