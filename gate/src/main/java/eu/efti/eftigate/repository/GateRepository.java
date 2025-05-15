package eu.efti.eftigate.repository;

import eu.efti.commons.enums.CountryIndicator;
import eu.efti.eftigate.entity.GateEntity;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;

public interface GateRepository extends JpaRepository<GateEntity, Long> {
    @QueryHints({@QueryHint(name = HINT_CACHEABLE, value = "true")})
    List<GateEntity> findByCountryIn(List<CountryIndicator> countries);

    @QueryHints({@QueryHint(name = HINT_CACHEABLE, value = "true")})
    GateEntity findByGateId(String gateId);

    @Override
    @QueryHints({@QueryHint(name = HINT_CACHEABLE, value = "true")})
    List<GateEntity> findAll();
}
