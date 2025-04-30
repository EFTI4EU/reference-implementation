package eu.efti.identifiersregistry.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
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
    final Consignment firstConsignment = new Consignment();
    final Consignment secondConsignment = new Consignment();
    final Consignment thirdConsignment = new Consignment();
    final Consignment fourthConsignment = new Consignment();
    final Consignment fifthConsignment = new Consignment();
    final Consignment sixthConsignment = new Consignment();


    @BeforeEach
    public void before() {

        CarriedTransportEquipment firstCarriedTransportEquipment = CarriedTransportEquipment.builder()
                .sequenceNumber(1)
                .equipmentId("67890")
                .schemeAgencyId("UN")
                .build();
        UsedTransportEquipment firstUsedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("54321")
                .registrationCountry("FR")
                .categoryCode("AE")
                .sequenceNumber(2)
                .schemeAgencyId("UN")
                .carriedTransportEquipments(List.of(firstCarriedTransportEquipment))
                .build();
        MainCarriageTransportMovement firstMainCarriageTransportMovement = MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false)
                .modeCode("3")
                .usedTransportMeansId("FMC888")
                .usedTransportMeansRegistrationCountry("FR")
                .schemeAgencyId("UN")
                .build();
        firstConsignment.setGateId("france");
        firstConsignment.setPlatformId("acme");
        firstConsignment.setDatasetId("67676767-6767-6767-6767-11180123be5b");
        firstConsignment.setMainCarriageTransportMovements(List.of(firstMainCarriageTransportMovement));
        firstConsignment.setUsedTransportEquipments(List.of(firstUsedTransportEquipment));
        firstConsignment.setDisabledDate(OffsetDateTime.now().plusYears(1));

        identifiersRepository.save(firstConsignment);

        CarriedTransportEquipment secondCarriedTransportEquipment = CarriedTransportEquipment.builder()
                .sequenceNumber(1)
                .equipmentId("67890")
                .schemeAgencyId("UN")
                .build();
        UsedTransportEquipment secondUsedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("54321")
                .registrationCountry("FR")
                .categoryCode("AE")
                .sequenceNumber(2)
                .schemeAgencyId("UN")
                .carriedTransportEquipments(List.of(secondCarriedTransportEquipment))
                .build();
        MainCarriageTransportMovement secondMainCarriageTransportMovement = MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false)
                .modeCode("3")
                .usedTransportMeansId("FMC888")
                .usedTransportMeansRegistrationCountry("FR")
                .schemeAgencyId("UN")
                .build();

        secondConsignment.setGateId("france");
        secondConsignment.setPlatformId("acme");
        secondConsignment.setDatasetId("67676767-6767-6767-6767-22280123be5b");
        secondConsignment.setMainCarriageTransportMovements(List.of(secondMainCarriageTransportMovement));
        secondConsignment.setUsedTransportEquipments(List.of(secondUsedTransportEquipment));
        secondConsignment.setDisabledDate(OffsetDateTime.now().plusYears(1));
        identifiersRepository.save(secondConsignment);

        CarriedTransportEquipment thirdCarriedTransportEquipment = CarriedTransportEquipment.builder()
                .sequenceNumber(1)
                .equipmentId("67890")
                .schemeAgencyId("UN")
                .build();
        UsedTransportEquipment thirdUsedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("54321")
                .registrationCountry("FR")
                .categoryCode("AE")
                .sequenceNumber(2)
                .schemeAgencyId("UN")
                .carriedTransportEquipments(List.of(thirdCarriedTransportEquipment))
                .build();
        MainCarriageTransportMovement thirdMainCarriageTransportMovement = MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false)
                .modeCode("3")
                .usedTransportMeansId("FMC888")
                .usedTransportMeansRegistrationCountry("IT")
                .schemeAgencyId("UN")
                .build();

        thirdConsignment.setGateId("france");
        thirdConsignment.setPlatformId("acme");
        thirdConsignment.setDatasetId("67676767-6767-6767-6767-33380123be5b");
        thirdConsignment.setMainCarriageTransportMovements(List.of(thirdMainCarriageTransportMovement));
        thirdConsignment.setUsedTransportEquipments(List.of(thirdUsedTransportEquipment));
        thirdConsignment.setDisabledDate(OffsetDateTime.now().plusYears(1));
        identifiersRepository.save(thirdConsignment);

        CarriedTransportEquipment fourthCarriedTransportEquipment = CarriedTransportEquipment.builder()
                .sequenceNumber(1)
                .equipmentId("67890")
                .schemeAgencyId("UN")
                .build();
        UsedTransportEquipment fourthUsedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("54321")
                .registrationCountry("FR")
                .categoryCode("AE")
                .sequenceNumber(2)
                .schemeAgencyId("UN")
                .carriedTransportEquipments(List.of(fourthCarriedTransportEquipment))
                .build();
        MainCarriageTransportMovement fourthMainCarriageTransportMovement = MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false)
                .modeCode("3")
                .usedTransportMeansId("ASB-123")
                .usedTransportMeansRegistrationCountry("FI")
                .schemeAgencyId("UN")
                .build();

        fourthConsignment.setGateId("finland");
        fourthConsignment.setPlatformId("syldavia");
        fourthConsignment.setDatasetId("67676767-6767-6767-6767-44480123be5b");
        fourthConsignment.setMainCarriageTransportMovements(List.of(fourthMainCarriageTransportMovement));
        fourthConsignment.setUsedTransportEquipments(List.of(fourthUsedTransportEquipment));
        fourthConsignment.setDisabledDate(OffsetDateTime.now().plusYears(1));
        identifiersRepository.save(fourthConsignment);

        CarriedTransportEquipment fifthCarriedTransportEquipment = CarriedTransportEquipment.builder()
                .sequenceNumber(1)
                .equipmentId("ASB-123")
                .schemeAgencyId("UN")
                .build();
        UsedTransportEquipment fifthUsedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("54321")
                .registrationCountry("FR")
                .categoryCode("AE")
                .sequenceNumber(2)
                .schemeAgencyId("UN")
                .carriedTransportEquipments(List.of(fifthCarriedTransportEquipment))
                .build();
        fifthCarriedTransportEquipment.setUsedTransportEquipment(fifthUsedTransportEquipment);
        MainCarriageTransportMovement fifthMainCarriageTransportMovement = MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false)
                .modeCode("3")
                .usedTransportMeansId("ASB-123")
                .usedTransportMeansRegistrationCountry("DE")
                .schemeAgencyId("UN")
                .build();

        fifthConsignment.setGateId("italy");
        fifthConsignment.setPlatformId("listenbourg");
        fifthConsignment.setDatasetId("67676767-6767-6767-6767-55580123be5b");
        fifthConsignment.setMainCarriageTransportMovements(List.of(fifthMainCarriageTransportMovement));
        fifthConsignment.setUsedTransportEquipments(List.of(fifthUsedTransportEquipment));
        fifthConsignment.setDisabledDate(OffsetDateTime.now().plusYears(1));
        identifiersRepository.save(fifthConsignment);

        CarriedTransportEquipment sixthCarriedTransportEquipment = CarriedTransportEquipment.builder()
                .sequenceNumber(1)
                .equipmentId("67890")
                .schemeAgencyId("UN")
                .build();
        UsedTransportEquipment sixthUsedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("ASB-123")
                .registrationCountry("FI")
                .categoryCode("AE")
                .sequenceNumber(2)
                .schemeAgencyId("UN")
                .carriedTransportEquipments(List.of(sixthCarriedTransportEquipment))
                .build();
        MainCarriageTransportMovement sixthMainCarriageTransportMovement = MainCarriageTransportMovement.builder()
                .dangerousGoodsIndicator(false)
                .modeCode("3")
                .usedTransportMeansId("BBB-345")
                .usedTransportMeansRegistrationCountry("DE")
                .schemeAgencyId("UN")
                .build();

        sixthConsignment.setGateId("danemark");
        sixthConsignment.setPlatformId("umbrellainc");
        sixthConsignment.setDatasetId("67676767-6767-6767-6767-66680123be5b");
        sixthConsignment.setMainCarriageTransportMovements(List.of(sixthMainCarriageTransportMovement));
        sixthConsignment.setUsedTransportEquipments(List.of(sixthUsedTransportEquipment));
        sixthConsignment.setDisabledDate(OffsetDateTime.now().plusYears(1));
        identifiersRepository.save(sixthConsignment);

    }

    @Test
    void shouldGetDataByUil() {

        final Optional<Consignment> result = identifiersRepository.findActiveByUil("france", "67676767-6767-6767-6767-11180123be5b", "acme");
        final Optional<Consignment> otherResult = identifiersRepository.findActiveByUil("finland", "67676767-6767-6767-6767-44480123be5b", "syldavia");
        final Optional<Consignment> emptyResult = identifiersRepository.findActiveByUil("notgateid", "thedatauuid", "acme");

        assertTrue(result.isPresent());
        assertEquals("france", result.get().getGateId());
        assertTrue(otherResult.isPresent());
        assertEquals("finland", otherResult.get().getGateId());
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void shouldGetDataByRegistrationCountryCriteria() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("FMC888").modeCode("3")
                .registrationCountryCode("FR")
                .identifierType(List.of("means"))
                .eftiGateIndicator(List.of("FR"))
                .build(), true);

        assertEquals(2, foundConsignments.size());
        assertTrue(foundConsignments.stream().anyMatch(c -> c.getDatasetId().equals("67676767-6767-6767-6767-11180123be5b")));
        assertTrue(foundConsignments.stream().anyMatch(c -> c.getDatasetId().equals("67676767-6767-6767-6767-22280123be5b")));
    }

    @Test
    void shouldGetDataByOneIdentifierTypeAndIdCriteria() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("ASB-123")
                .identifierType(List.of("means"))
                .build(), true);

        assertEquals(2, foundConsignments.size());
        assertTrue(CollectionUtils.isEqualCollection(foundConsignments, List.of(fourthConsignment, fifthConsignment)));
    }

    @Test
    void shouldGetDataByOneIdentifierTypeAndIdAndCountryCriteria() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("ASB-123")
                .identifierType(List.of("means"))
                .registrationCountryCode("FI")
                .build(), true);

        assertEquals(1, foundConsignments.size());
        assertEquals("67676767-6767-6767-6767-44480123be5b", foundConsignments.iterator().next().getDatasetId());
    }

    @Test
    void shouldGetDataByTwoIdentifierTypeAndIdAndCountryCriteria() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("ASB-123")
                .identifierType(List.of("means", "equipment"))
                .registrationCountryCode("FI")
                .build(), true);

        assertEquals(2, foundConsignments.size());
        assertTrue(CollectionUtils.isEqualCollection(foundConsignments, List.of(fourthConsignment, sixthConsignment)));
    }

    @Test
    void shouldGetDataByThreeIdentifierTypeAndIdAndCountryCriteria() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("ASB-123")
                .identifierType(List.of("means", "carried", "equipment"))
                .registrationCountryCode("FI")
                .build(), true);

        assertEquals(3, foundConsignments.size());
        assertTrue(CollectionUtils.isEqualCollection(foundConsignments, List.of(fourthConsignment, fifthConsignment, sixthConsignment)));
    }

    @Test
    void shouldSearchDataUsingAllIdentifierTypeWhenGivenIdentifierTypesAreEmpty() {
        List<Consignment> foundConsignments = identifiersRepository.searchByCriteria(SearchWithIdentifiersRequestDto.builder()
                .identifier("FMC888")
                .registrationCountryCode("FR")
                .build(), true);

        assertEquals(2, foundConsignments.size());
        assertTrue(CollectionUtils.isEqualCollection(foundConsignments, List.of(firstConsignment, secondConsignment)));
    }
}
