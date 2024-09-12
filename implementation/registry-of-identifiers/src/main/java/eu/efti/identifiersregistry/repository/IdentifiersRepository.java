package eu.efti.identifiersregistry.repository;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.TransportMode;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import jakarta.persistence.criteria.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface IdentifiersRepository extends JpaRepository<Consignment, Long>, JpaSpecificationExecutor<Consignment> {

    String VEHICLE_COUNTRY = "registrationCountry";
    String TRANSPORT_MODE = "modeCode";
    String IS_DANGEROUS_GOODS = "dangerousGoodsIndicator";
    String TRANSPORT_VEHICLES = "usedTransportEquipments";
    String VEHICLE_ID = "equipmentId";

    @Query(value = "SELECT c FROM Consignment c where c.gateId = :gate and c.datasetId = :uuid and c.platformId = :platform")
    Optional<Consignment> findByUil(final String gate, final String uuid, final String platform);

    default List<Consignment> searchByCriteria(final SearchWithIdentifiersRequestDto request) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (request.getIsDangerousGoods() != null) {
                Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join("mainCarriageTransportMovements", JoinType.LEFT);
                predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(IS_DANGEROUS_GOODS), request.getIsDangerousGoods())));
            }
            if (StringUtils.isNotEmpty(request.getTransportMode())) {
                Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join("mainCarriageTransportMovements", JoinType.LEFT);
                predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(TRANSPORT_MODE), TransportMode.valueOf(request.getTransportMode()))));
            }
            //vehicle subquery
            predicates.add(buildSubQuery(request, cb, root));

            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    private Predicate buildSubQuery(final SearchWithIdentifiersRequestDto request, final CriteriaBuilder cb, final Root<Consignment> root) {
        final Join<Consignment, UsedTransportEquipment> vehicles = root.join(TRANSPORT_VEHICLES);
        final List<Predicate> subQueryPredicate = new ArrayList<>();
        subQueryPredicate.add(cb.equal(cb.upper(vehicles.get(VEHICLE_ID)), request.getVehicleID().toUpperCase()));
        if (StringUtils.isNotEmpty(request.getVehicleCountry())) {
            subQueryPredicate.add(cb.equal(vehicles.get(VEHICLE_COUNTRY), CountryIndicator.valueOf(request.getVehicleCountry()).toString()));
        if (StringUtils.isNotEmpty(request.getTransportMode())) {
            subQueryPredicate.add(cb.equal(vehicles.get(TRANSPORT_MODE), TransportMode.valueOf(request.getTransportMode())));
        }
        return cb.and(subQueryPredicate.toArray(new Predicate[]{}));
    }
}
