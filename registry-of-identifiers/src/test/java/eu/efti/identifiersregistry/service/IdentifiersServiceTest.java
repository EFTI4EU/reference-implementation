package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.eftilogger.service.ReportingRegistryLogService;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.identifiersregistry.utils.OffsetDateTimeDeserializer;
import eu.efti.v1.consignment.identifier.LogisticsTransportMovement;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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
    @Mock
    private ReportingRegistryLogService reportingRegistryLogService;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        service = new IdentifiersService(repository, mapperUtils, auditRegistryLogService, serializeUtils,reportingRegistryLogService);

        ReflectionTestUtils.setField(service, "gateOwner", "france");
        ReflectionTestUtils.setField(service, "nullDeliveryDateMaxDayPassed", 90);

        SaveIdentifiersRequest identifiersRequest = defaultSaveIdentifiersRequest();
        saveIdentifiersRequestWrapper = new SaveIdentifiersRequestWrapper(PLATFORM_ID, identifiersRequest);
        LogisticsTransportMovement logisticsTransportMovement = new LogisticsTransportMovement();
        logisticsTransportMovement.setModeCode("3");
        logisticsTransportMovement.setDangerousGoodsIndicator(false);
        saveIdentifiersRequestWrapper.getSaveIdentifiersRequest().getConsignment().getMainCarriageTransportMovement().add(logisticsTransportMovement);


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
    void deleteOldConsignmentTest() {
        when(repository.deleteAllDisabledConsignment()).thenReturn(1);

        int result = service.deleteOldConsignment();

        assertEquals(1, result);
    }

    @Test()
    void deleteOldConsignmentThrowExceptionTest() {
        doThrow(new IllegalArgumentException("Bad argument")).when(repository).deleteAllDisabledConsignment();

        int result = service.deleteOldConsignment();

        assertEquals(0, result);
    }

    @Test
    void setDisabledDateModeCodeOneTest() {
        MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
        mainCarriageTransportMovement.setModeCode("1");
        consignment.setMainCarriageTransportMovements(List.of(mainCarriageTransportMovement));
        consignment.setDeliveryEventActualOccurrenceDatetime(OffsetDateTime.now(Clock.systemUTC()));

        Consignment result = service.setDisabledDate(consignment);

        assertNotNull(result.getDisabledDate());
    }

    @Test
    void setDisabledDateModeCodeThreeTest() {
        MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
        mainCarriageTransportMovement.setModeCode("3");
        consignment.setMainCarriageTransportMovements(List.of(mainCarriageTransportMovement));
        consignment.setDeliveryEventActualOccurrenceDatetime(OffsetDateTime.now(Clock.systemUTC()));

        Consignment result = service.setDisabledDate(consignment);

        assertNotNull(result.getDisabledDate());
    }

    @Test
    void setDisabledDateModeCodeThreeNoDeliveryDateTest() {
        MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
        mainCarriageTransportMovement.setModeCode("3");
        consignment.setMainCarriageTransportMovements(List.of(mainCarriageTransportMovement));

        Consignment result = service.setDisabledDate(consignment);

        assertNotNull(result.getDisabledDate());
    }

    @Test
    void setDisabledDateModeCodeThreeNoDeliveryDateButAcceptanceDateTest() {
        MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
        mainCarriageTransportMovement.setModeCode("3");
        consignment.setMainCarriageTransportMovements(List.of(mainCarriageTransportMovement));
        consignment.setCarrierAcceptanceDatetime(OffsetDateTime.now());

        Consignment result = service.setDisabledDate(consignment);

        assertNotNull(result.getDisabledDate());
    }

    @Test
    void setDisabledDateModeCodeThreeNoDeliveryDateButCreatedDateTest() {
        MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
        mainCarriageTransportMovement.setModeCode("3");
        consignment.setMainCarriageTransportMovements(List.of(mainCarriageTransportMovement));
        consignment.setCreatedDate(LocalDateTime.now());

        Consignment result = service.setDisabledDate(consignment);

        assertNotNull(result.getDisabledDate());
    }

    @Test
    void shouldCreateIdentifiers() {
        when(repository.save(any())).thenReturn(consignment);
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService, times(2)).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any(), any(), any(), any(), any());
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
        verify(auditRegistryLogService, times(2)).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any(), any(), any(), any(), any());
        assertEquals("wrong value", argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_ID, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_ID, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldCreateIfUilNotFound() {
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findActiveByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.empty());
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService, times(2)).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any(), any(), any(), any(), any());
        verify(repository).findByUil(GATE_ID, DATA_UUID, PLATFORM_ID);
        assertEquals(DATA_UUID, argumentCaptor.getValue().getDatasetId());
        assertEquals(PLATFORM_ID, argumentCaptor.getValue().getPlatformId());
        assertEquals(GATE_ID, argumentCaptor.getValue().getGateId());
    }

    @Test
    void shouldFindByUil() {
        when(repository.findActiveByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.of(new Consignment()));

        assertNotNull(service.findByUIL(DATA_UUID, GATE_ID, PLATFORM_ID));
    }

    @Test
    void shouldNotFindByUil() {
        when(repository.findActiveByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.empty());

        assertNull(service.findByUIL(DATA_UUID, GATE_ID, PLATFORM_ID));
    }

    @Test
    void shouldUpdateIfUILFound() {
        when(repository.save(any())).thenReturn(consignment);
        when(repository.findActiveByUil(GATE_ID, DATA_UUID, PLATFORM_ID)).thenReturn(Optional.of(new Consignment()));
        final ArgumentCaptor<Consignment> argumentCaptor = ArgumentCaptor.forClass(Consignment.class);

        service.createOrUpdate(saveIdentifiersRequestWrapper);

        verify(repository).save(argumentCaptor.capture());
        verify(auditRegistryLogService, times(2)).log(any(SaveIdentifiersRequestWrapper.class), any(), any(), any(), any(), any(), any(), any(), any());
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
