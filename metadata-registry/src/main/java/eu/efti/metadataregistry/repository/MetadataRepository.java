package eu.efti.metadataregistry.repository;

import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.TransportMode;
import eu.efti.metadataregistry.entity.MetadataEntity;
import eu.efti.metadataregistry.entity.TransportVehicle;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface MetadataRepository extends JpaRepository<MetadataEntity, Long>, JpaSpecificationExecutor<MetadataEntity> {

    String VEHICLE_COUNTRY = "vehicleCountry";
    String TRANSPORT_MODE = "transportMode";
    String IS_DANGEROUS_GOODS = "isDangerousGoods";
    String TRANSPORT_VEHICLES = "transportVehicles";
    String VEHICLE_ID = "vehicleID";

    @Query(value = "SELECT m FROM MetadataEntity m where m.eFTIGateUrl = :gate and m.eFTIDataUuid = :uuid and m.eFTIPlatformUrl = :platform")
    Optional<MetadataEntity> findByUil(final String gate, final String uuid, final String platform);

    default List<MetadataEntity> searchByCriteria(final MetadataRequestDto request) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if(request.getIsDangerousGoods() != null) {
                predicates.add(cb.equal(root.get(IS_DANGEROUS_GOODS), request.getIsDangerousGoods()));
            }
            //vehicle subquery
            predicates.add(buildSubQuery(request, cb, root));

            return cb.and(predicates.toArray(new Predicate[] {}));
        });
    }

    private Predicate buildSubQuery(final MetadataRequestDto request, final CriteriaBuilder cb, final Root<MetadataEntity> root) {
        final Join<MetadataEntity, TransportVehicle> vehicles = root.join(TRANSPORT_VEHICLES);
        final List<Predicate> subQueryPredicate = new ArrayList<>();

        subQueryPredicate.add(cb.equal(cb.upper(vehicles.get(VEHICLE_ID)), request.getVehicleID().toUpperCase()));
        if(StringUtils.isNotEmpty(request.getTransportMode())) {
            subQueryPredicate.add(cb.equal(vehicles.get(TRANSPORT_MODE), TransportMode.valueOf(request.getTransportMode())));
        }
        if(StringUtils.isNotEmpty(request.getVehicleCountry())) {
            subQueryPredicate.add(cb.equal(vehicles.get(VEHICLE_COUNTRY), CountryIndicator.valueOf(request.getVehicleCountry())));
        }
        return cb.and(subQueryPredicate.toArray(new Predicate[] {}));
    }
}
