package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IdentifiersRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"eu.efti.identifiersregistry.repository"})
@EntityScan("eu.efti.identifiersregistry.entity")
class IdentifiersRepositoryTest {

    @Autowired
    private IdentifiersRepository identifiersRepository;

    AutoCloseable openMocks;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);

        final Consignment consignment = new Consignment();
        consignment.setGateId("thegateurl");
        consignment.setDatasetId("thedatauuid");
        consignment.setPlatformId("theplatformurl");

        consignment.setMainCarriageTransportMovements(List.of(MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(true)
                .build()));

        var usedTransportEquipments = List.of(UsedTransportEquipment.builder()
                        .equipmentId("vehicleId1")
                        .registrationCountry(CountryIndicator.FR.name())
                        .build(),
                UsedTransportEquipment.builder()
                        .equipmentId("vehicleId2")
                        .registrationCountry(CountryIndicator.CY.name())
                        .build());
        consignment.setUsedTransportEquipments(usedTransportEquipments);
        identifiersRepository.save(consignment);

        final Consignment otherConsignment = new Consignment();
        otherConsignment.setGateId("othergateurl");
        otherConsignment.setDatasetId("thedatauuid");
        otherConsignment.setPlatformId("theplatformurl");

        MainCarriageTransportMovement movement2 = new MainCarriageTransportMovement();
        movement2.setDangerousGoodsIndicator(false);
        otherConsignment.setMainCarriageTransportMovements(List.of(movement2));

        UsedTransportEquipment equipment3 = new UsedTransportEquipment();
        equipment3.setEquipmentId("vehicleId1");
        equipment3.setRegistrationCountry(CountryIndicator.FR.name());

        UsedTransportEquipment equipment4 = new UsedTransportEquipment();
        equipment4.setEquipmentId("vehicleId2");
        equipment4.setRegistrationCountry(CountryIndicator.FR.name());

        otherConsignment.setUsedTransportEquipments(List.of(equipment3, equipment4));

        identifiersRepository.save(otherConsignment);
    }

    @Test
    void shouldGetDataByUil() {

        final Optional<Consignment> result = identifiersRepository.findByUil("thegateurl", "thedatauuid", "theplatformurl");
        final Optional<Consignment> otherResult = identifiersRepository.findByUil("othergateurl", "thedatauuid", "theplatformurl");
        final Optional<Consignment> emptyResult = identifiersRepository.findByUil("notgateurl", "thedatauuid", "theplatformurl");

        assertTrue(result.isPresent());
        assertEquals("thegateurl", result.get().getGateId());
        assertTrue(otherResult.isPresent());
        assertEquals("othergateurl", otherResult.get().getGateId());
        assertTrue(emptyResult.isEmpty());

    }

    @Test
    void shouldGetDataByCriteria() {
        assertEquals(2, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .vehicleID("vehicleId1")
                .vehicleCountry(CountryIndicator.FR.name())
                .build()).size());

        assertEquals(1, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .vehicleID("vehicleId1")
                .vehicleCountry(CountryIndicator.FR.name())
                .isDangerousGoods(false)
                .build()).size());

        assertEquals(1, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .vehicleID("vehicleId2")
                .vehicleCountry(CountryIndicator.CY.name())
                .build()).size());

        assertEquals(2, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .vehicleID("vehicleId2")
                .build()).size());

        assertEquals(0, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .vehicleID("vehicleId2")
                .vehicleCountry(CountryIndicator.BE.name())
                .build()).size());

        assertEquals(0, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .vehicleID("vehicleId2")
                .vehicleCountry(CountryIndicator.CY.name())
                .identifierType(List.of("carried"))
                .build()).size());
    }

}
