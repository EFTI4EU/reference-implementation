package eu.efti.eftigate.service.gate;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.eftigate.entity.GateEntity;
import eu.efti.eftigate.repository.GateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EftiGateUrlResolverTest {
    private EftiGateUrlResolver eftiGateUrlResolver;
    @Mock
    private GateRepository gateRepository;

    private final MetadataRequestDto metadataRequestDto = new MetadataRequestDto();

    GateEntity frGateEntity;
    GateEntity beGateEntity;
    GateEntity deGateEntity;

    @BeforeEach
    public void before() {
        eftiGateUrlResolver = new EftiGateUrlResolver(gateRepository);

        final AuthorityDto authorityDto = new AuthorityDto();

        this.metadataRequestDto.setVehicleID("abc123");
        this.metadataRequestDto.setVehicleCountry("FR");
        this.metadataRequestDto.setAuthority(authorityDto);
        this.metadataRequestDto.setTransportMode("ROAD");

        frGateEntity = GateEntity.builder().id(1L).url("https://efti.gate.fr.eu").country(CountryIndicator.FR).build();
        beGateEntity = GateEntity.builder().id(2L).url("https://efti.gate.be.eu").country(CountryIndicator.BE).build();
        deGateEntity = GateEntity.builder().id(3L).url("https://efti.gate.de.eu").country(CountryIndicator.DE).build();
    }

    @Test
    void shouldResolveGates_WhenCountryIndicatorsAreGiven(){
        //Arrange
        this.metadataRequestDto.setEFTIGateIndicator(List.of("BE", "FR"));
        when(gateRepository.findByCountryIn(anyList())).thenReturn(List.of(frGateEntity, beGateEntity));

        //Act
        final List<String> destinationGates = eftiGateUrlResolver.resolve(metadataRequestDto);

        //Assert
        assertFalse(destinationGates.isEmpty());
        assertThat(List.of(frGateEntity.getUrl(), beGateEntity.getUrl())).hasSameElementsAs(destinationGates);
    }

    @Test
    void shouldGetAllGates_WhenCountryIndicatorsAreNotGiven(){
        //Arrange
        when(gateRepository.findAll()).thenReturn(List.of(frGateEntity, beGateEntity, deGateEntity));

        //Act
        final List<String> destinationGates = eftiGateUrlResolver.resolve(metadataRequestDto);

        //Assert
        assertFalse(destinationGates.isEmpty());
        assertThat(List.of(frGateEntity.getUrl(), beGateEntity.getUrl(), deGateEntity.getUrl())).hasSameElementsAs(destinationGates);
    }
}
