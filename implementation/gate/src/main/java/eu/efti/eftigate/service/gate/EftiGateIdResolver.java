package eu.efti.eftigate.service.gate;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.eftigate.entity.GateEntity;
import eu.efti.eftigate.repository.GateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class EftiGateIdResolver {

    private final GateRepository gateRepository;

    public List<String> resolve(final SearchWithIdentifiersRequestDto identifiersRequestDto) {
        final Map<CountryIndicator, GateEntity> destinationGatesIndicatorMap;

        if (CollectionUtils.isNotEmpty(identifiersRequestDto.getEftiGateIndicator())) {
            final List<CountryIndicator> requestedCountryIndicators = identifiersRequestDto.getEftiGateIndicator().stream().map(CountryIndicator::valueOf).toList();
            final List<GateEntity> registeredDestinationGates = gateRepository.findByCountryIn(requestedCountryIndicators);
            destinationGatesIndicatorMap = mapRequestedCountriesToRegisteredGates(requestedCountryIndicators, registeredDestinationGates);
        } else {
            destinationGatesIndicatorMap = CollectionUtils.emptyIfNull(gateRepository.findAll())
                    .stream()
                    .collect(Collectors.toMap(GateEntity::getCountry, Function.identity()));
        }

        return destinationGatesIndicatorMap.values()
                .stream()
                .map(gateEntity -> gateEntity != null ? gateEntity.getGateId() : null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public String resolve(final String gateId) {
        final GateEntity gateEntity = gateRepository.findByGateId(gateId);
        return gateEntity != null ? gateEntity.getCountry().name() : null;
    }

    private Map<CountryIndicator, GateEntity> mapRequestedCountriesToRegisteredGates(final List<CountryIndicator> requestedCountryIndicators, final List<GateEntity> registeredDestinationGates) {
        final Map<CountryIndicator, GateEntity> destinationGatesIndicatorMap = new EnumMap<>(CountryIndicator.class);

        CollectionUtils.emptyIfNull(requestedCountryIndicators).forEach(countryIndicator -> {
            final GateEntity foundedRegisteredGated = CollectionUtils.emptyIfNull(registeredDestinationGates).stream()
                    .filter(registeredGate -> countryIndicator.equals(registeredGate.getCountry()))
                    .findFirst()
                    .orElse(null);
            destinationGatesIndicatorMap.put(countryIndicator, foundedRegisteredGated);
        });
        return destinationGatesIndicatorMap;
    }
}
