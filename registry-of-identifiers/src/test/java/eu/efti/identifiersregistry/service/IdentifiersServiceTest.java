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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentifiersServiceTest extends AbstractServiceTest {

    public static final String GATE_ID = "france";
    public static final String DATA_UUID = "12345678-ab12-4ab6-8999-123456789abc";
    public static final String PLATFORM_ID = "ttf";
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

        ReflectionTestUtils.setField(service, "gateOwner", "france");
        ReflectionTestUtils.setField(service, "gateCountry", "BO");

        SaveIdentifiersRequest identifiersRequest = defaultSaveIdentifiersRequest();
        saveIdentifiersRequestWrapper = new SaveIdentifiersRequestWrapper(PLATFORM_ID, identifiersRequest);


        consignment = new Consignment();
        consignment.setGateId(GATE_ID);
        consignment.setDatasetId(DATA_UUID);
        consignment.setPlatformId(PLATFORM_ID);
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
        assertEquals(PLATFORM_ID, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_ID, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldCreateIdentifiersAndIgnoreWrongsFields() {
        saveIdentifiersRequestWrapper.getSaveIdentifiersRequest().setDatasetId("wrong value");
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        assertEquals("wrong value", argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_ID, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_ID, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldCreateIfUilNotFound() {
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.empty());
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        verify(repository).findByUil(GATE_ID, DATA_UUID, PLATFORM_ID);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_ID, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_ID, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldExistByUil() {
        when(repository.findByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.of(new Consignment()));

        assertTrue(service.existByUIL(DATA_UUID, GATE_ID, PLATFORM_ID));
    }

    @Test
    void shouldNotExistByUil() {
        when(repository.findByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.empty());

        assertFalse(service.existByUIL(DATA_UUID, GATE_ID, PLATFORM_ID));
    }

    @Test
    void shouldUpdateIfUILFound() {
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.of(new Consignment()));
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any());
        verify(repository).findByUil(GATE_ID, DATA_UUID, PLATFORM_ID);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_ID, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_ID, argumentCaptor.getValue().getGateId());
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
