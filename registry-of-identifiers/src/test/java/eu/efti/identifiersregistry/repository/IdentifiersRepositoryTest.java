package eu.efti.identifiersregistry.repository;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.TransportMode;
import eu.efti.identifiersregistry.entity.Identifiers;
import eu.efti.identifiersregistry.entity.TransportVehicle;
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
@EnableJpaRepositories(basePackages = {"eu.efti.identifiersregistry.repository"})
@EntityScan("eu.efti.identifiersregistry.entity")
class IdentifiersRepositoryTest {

    @Autowired
    private IdentifiersRepository identifiersRepository;

    AutoCloseable openMocks;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);

        final Identifiers identifiers = Identifiers.builder()
                .eFTIGateUrl("thegateurl")
                .eFTIDataUuid("thedatauuid")
                .eFTIPlatformUrl("theplatformurl")
                .identifiersUUID(UUID.randomUUID().toString())
                .isDangerousGoods(true)
                .transportVehicles(List.of(TransportVehicle.builder()
                                .vehicleID("vehicleID1")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.FR)
                                .build(),
                        TransportVehicle.builder()
                                .vehicleID("vehicleID2")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.CY)
                                .build()))
                .build();

        final Identifiers otherIdentifiers = Identifiers.builder()
                .eFTIGateUrl("othergateurl")
                .eFTIDataUuid("thedatauuid")
                .eFTIPlatformUrl("theplatformurl")
                .identifiersUUID(UUID.randomUUID().toString())
                .isDangerousGoods(false)
                .transportVehicles(List.of(TransportVehicle.builder()
                                .vehicleID("vehicleID1")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.FR)
                                .build(),
                        TransportVehicle.builder()
                                .vehicleID("vehicleID2")
                                .transportMode(TransportMode.ROAD)
                                .vehicleCountry(CountryIndicator.FR)
                                .build()))
                .build();
        identifiersRepository.save(identifiers);
        identifiersRepository.save(otherIdentifiers);
    }

    @Test
    void shouldGetDataByUil() {

        final Optional<Identifiers> result = identifiersRepository.findByUil("thegateurl", "thedatauuid", "theplatformurl");
        final Optional<Identifiers> otherResult = identifiersRepository.findByUil("othergateurl", "thedatauuid", "theplatformurl");
        final Optional<Identifiers> emptyResult = identifiersRepository.findByUil("notgateurl", "thedatauuid", "theplatformurl");

        assertTrue(result.isPresent());
        assertEquals("thegateurl", result.get().getEFTIGateUrl());
        assertTrue(otherResult.isPresent());
        assertEquals("othergateurl", otherResult.get().getEFTIGateUrl());
        assertTrue(emptyResult.isEmpty());

    }

    @Test
    void shouldGetDataByCriteria() {
        final SearchWithIdentifiersRequestDto identifiersRequestDto = SearchWithIdentifiersRequestDto.builder().vehicleID("vehicleId1").vehicleCountry(CountryIndicator.FR.name()).build();
        final List<Identifiers> result = identifiersRepository.searchByCriteria(identifiersRequestDto);
        assertEquals(2, result.size());

        final SearchWithIdentifiersRequestDto identifiersRequestDto2 = SearchWithIdentifiersRequestDto.builder().vehicleID("vehicleId1")
                .vehicleCountry(CountryIndicator.FR.name()).isDangerousGoods(false).build();
        final List<Identifiers> result2 = identifiersRepository.searchByCriteria(identifiersRequestDto2);
        assertEquals(1, result2.size());
    }

}
