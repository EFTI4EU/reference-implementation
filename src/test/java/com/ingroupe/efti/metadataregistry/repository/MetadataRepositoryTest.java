package com.ingroupe.efti.metadataregistry.repository;

import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.commons.enums.TransportMode;
import com.ingroupe.efti.metadataregistry.entity.MetadataEntity;
import com.ingroupe.efti.metadataregistry.entity.TransportVehicle;
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
@ContextConfiguration(classes= {MetadataRepository.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableJpaRepositories(basePackages = {"com.ingroupe.efti.metadataregistry.repository"})
@EntityScan("com.ingroupe.efti.metadataregistry.entity")
class MetadataRepositoryTest {

    @Autowired
    private MetadataRepository metadataRepository;

    AutoCloseable openMocks;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);

        final MetadataEntity metadata = MetadataEntity.builder()
                .eFTIGateUrl("thegateurl")
                .eFTIDataUuid("thedatauuid")
                .eFTIPlatformUrl("theplatformurl")
                .metadataUUID(UUID.randomUUID().toString())
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

        final MetadataEntity otherMetadata = MetadataEntity.builder()
                .eFTIGateUrl("othergateurl")
                .eFTIDataUuid("thedatauuid")
                .eFTIPlatformUrl("theplatformurl")
                .metadataUUID(UUID.randomUUID().toString())
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
        metadataRepository.save(metadata);
        metadataRepository.save(otherMetadata);
    }

    @Test
    void shouldGetDataByUil() {

        final Optional<MetadataEntity> result = metadataRepository.findByUil("thegateurl", "thedatauuid", "theplatformurl");
        final Optional<MetadataEntity> otherResult = metadataRepository.findByUil("othergateurl", "thedatauuid", "theplatformurl");
        final Optional<MetadataEntity> emptyResult = metadataRepository.findByUil("notgateurl", "thedatauuid", "theplatformurl");

        assertTrue(result.isPresent());
        assertEquals("thegateurl", result.get().getEFTIGateUrl());
        assertTrue(otherResult.isPresent());
        assertEquals("othergateurl", otherResult.get().getEFTIGateUrl());
        assertTrue(emptyResult.isEmpty());

    }

    @Test
    void shouldGetDataByCriteria() {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder().vehicleID("vehicleId1").vehicleCountry(CountryIndicator.FR.name()).build();
        final List<MetadataEntity> result = metadataRepository.searchByCriteria(metadataRequestDto);
        assertEquals(2, result.size());

        final MetadataRequestDto metadataRequestDto2 = MetadataRequestDto.builder().vehicleID("vehicleId1")
                .vehicleCountry(CountryIndicator.FR.name()).isDangerousGoods(false).build();
        final List<MetadataEntity> result2 = metadataRepository.searchByCriteria(metadataRequestDto2);
        assertEquals(1, result2.size());
    }

}
