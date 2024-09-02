package eu.efti.identifierregistry.service;

import eu.efti.commons.dto.ConsignmentIdentifiersRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.TransportMode;
import eu.efti.identifierregistry.entity.Consignment;
import eu.efti.identifierregistry.entity.TransportVehicle;
import eu.efti.identifierregistry.repository.IdentifiersRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {IdentifiersRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"eu.efti.identifierregistry.repository"})
@EntityScan("eu.efti.identifierregistry.entity")
class IdentifiersRepositoryTest {

    @Autowired
    private IdentifiersRepository identifiersRepository;

    AutoCloseable openMocks;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);

        final Consignment consignment = Consignment.builder()
                .eFTIGateUrl("thegateurl")
                .eFTIDataUuid("thedatauuid")
                .eFTIPlatformUrl("theplatformurl")
                .consignmentUUID(UUID.randomUUID().toString())
                .isDangerousGoods(true)
                .transportVehicles(List.of(TransportVehicle.builder()
                                .vehicleId("vehicleId1")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.FR)
                                .build(),
                        TransportVehicle.builder()
                                .vehicleId("vehicleId2")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.CY)
                                .build()))
                .build();

        final Consignment otherConsignment = Consignment.builder()
                .eFTIGateUrl("othergateurl")
                .eFTIDataUuid("thedatauuid")
                .eFTIPlatformUrl("theplatformurl")
                .consignmentUUID(UUID.randomUUID().toString())
                .isDangerousGoods(false)
                .transportVehicles(List.of(TransportVehicle.builder()
                                .vehicleId("vehicleId1")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.FR)
                                .build(),
                        TransportVehicle.builder()
                                .vehicleId("vehicleId2")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.FR)
                                .build()))
                .build();
        identifiersRepository.save(consignment);
        identifiersRepository.save(otherConsignment);
    }

    @Test
    void shouldGetDataByUil() {

        final Optional<Consignment> result = identifiersRepository.findByUil("thegateurl", "thedatauuid", "theplatformurl");
        final Optional<Consignment> otherResult = identifiersRepository.findByUil("othergateurl", "thedatauuid", "theplatformurl");
        final Optional<Consignment> emptyResult = identifiersRepository.findByUil("notgateurl", "thedatauuid", "theplatformurl");

        assertTrue(result.isPresent());
        assertEquals("thegateurl", result.get().getEFTIGateUrl());
        assertTrue(otherResult.isPresent());
        assertEquals("othergateurl", otherResult.get().getEFTIGateUrl());
        assertTrue(emptyResult.isEmpty());

    }

    @Test
    void shouldGetDataByCriteria() {
        final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto = ConsignmentIdentifiersRequestDto.builder().vehicleID("vehicleId1").vehicleCountry(CountryIndicator.FR.name()).build();
        final List<Consignment> result = identifiersRepository.searchByCriteria(consignmentIdentifiersRequestDto);
        assertEquals(2, result.size());

        final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto2 = ConsignmentIdentifiersRequestDto.builder().vehicleID("vehicleId1")
                .vehicleCountry(CountryIndicator.FR.name()).isDangerousGoods(false).build();
        final List<Consignment> result2 = identifiersRepository.searchByCriteria(consignmentIdentifiersRequestDto2);
        assertEquals(1, result2.size());
    }

}
