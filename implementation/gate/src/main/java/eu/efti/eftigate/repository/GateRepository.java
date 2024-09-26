package eu.efti.eftigate.repository;

import eu.efti.commons.enums.CountryIndicator;
import eu.efti.eftigate.entity.GateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GateRepository extends JpaRepository<GateEntity, Long> {
    List<GateEntity> findByCountryIn(List<CountryIndicator> countries);
    GateEntity findByUrl(String url);
}
