package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
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


    @BeforeEach
    public void before() {

        final Consignment consignment = new Consignment();
        consignment.setGateId("thegateid");
        consignment.setDatasetId("thedatauuid");
        consignment.setPlatformId("theplatformid");

        consignment.setMainCarriageTransportMovements(List.of(MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(true)
                .build()));

        consignment.setUsedTransportEquipments(List.of(UsedTransportEquipment.builder()
                        .equipmentId("vehicleId1")
                        .registrationCountry(CountryIndicator.FR.name())
                        .categoryCode("AA")
                        .build(),
                UsedTransportEquipment.builder()
                        .equipmentId("vehicleId2")
                        .registrationCountry(CountryIndicator.CY.name())
                        .build()));
        identifiersRepository.save(consignment);

        final Consignment otherConsignment = new Consignment();
        otherConsignment.setGateId("othergateid");
        otherConsignment.setDatasetId("thedatauuid");
        otherConsignment.setPlatformId("theplatformid");

        otherConsignment.setMainCarriageTransportMovements(List.of(MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false).build()));

        UsedTransportEquipment equipment = UsedTransportEquipment.builder()
                .equipmentId("vehicleId1")
                .registrationCountry(CountryIndicator.FR.name())
                .build();
        equipment.setCarriedTransportEquipments(List.of(CarriedTransportEquipment.builder()
                .equipmentId("carriedId1")
                .build()));

        otherConsignment.setUsedTransportEquipments(List.of(equipment,
                UsedTransportEquipment.builder()
                        .equipmentId("vehicleId2")
                        .registrationCountry(CountryIndicator.FR.name()).build()));

        identifiersRepository.save(otherConsignment);
    }

    @Test
    void shouldGetDataByUil() {

        final Optional<Consignment> result = identifiersRepository.findByUil("thegateid", "thedatauuid", "theplatformid");
        final Optional<Consignment> otherResult = identifiersRepository.findByUil("othergateid", "thedatauuid", "theplatformid");
        final Optional<Consignment> emptyResult = identifiersRepository.findByUil("notgateid", "thedatauuid", "theplatformid");

        assertTrue(result.isPresent());
        assertEquals("thegateid", result.get().getGateId());
        assertTrue(otherResult.isPresent());
        assertEquals("othergateid", otherResult.get().getGateId());
        assertTrue(emptyResult.isEmpty());

    }

    @Test
    void shouldGetDataByCriteria() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("vehicleId1")
                .registrationCountryCode(CountryIndicator.FR.name())
                .build());

        assertTrue(foundConsignments.stream().anyMatch(c -> c.getUsedTransportEquipments().stream().anyMatch(e -> e.getCategoryCode().equals("AA"))),
                "One of the consignments should have categoryCode AA");
        assertEquals(2, foundConsignments.size());

        assertEquals(1, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("vehicleId1")
                .registrationCountryCode(CountryIndicator.FR.name())
                .dangerousGoodsIndicator(false)
                .build()).size());

        assertEquals(1, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("vehicleId2")
                .registrationCountryCode(CountryIndicator.CY.name())
                .build()).size());

        assertEquals(2, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("vehicleId2")
                .build()).size());

        assertEquals(0, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("vehicleId2")
                .registrationCountryCode(CountryIndicator.BE.name())
                .build()).size());

        assertEquals(0, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("vehicleId2")
                .registrationCountryCode(CountryIndicator.CY.name())
                .identifierType(List.of("carried"))
                .build()).size());

        assertEquals(1, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("carriedId1")
                .identifierType(List.of("carried"))
                .build()).size());

        assertEquals(1, identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("carriedId1")
                .build()).size());
    }

}
