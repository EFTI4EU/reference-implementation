package eu.efti.identifiersregistry.repository;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.TransportMode;
import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import jakarta.persistence.criteria.*;
import org.apache.commons.collections4.CollectionUtils;
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
    String MOVEMENTS = "mainCarriageTransportMovements";
    String TRANSPORT_VEHICLES = "usedTransportEquipments";
    String VEHICLE_ID = "equipmentId";

    @Query(value = "SELECT c FROM Consignment c where c.gateId = :gate and c.datasetId = :uuid and c.platformId = :platform")
    Optional<Consignment> findByUil(final String gate, final String uuid, final String platform);

    default List<Consignment> searchByCriteria(final SearchWithIdentifiersRequestDto request) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (request.getIsDangerousGoods() != null) {
                Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);
                predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(IS_DANGEROUS_GOODS), request.getIsDangerousGoods())));
            }
            if (StringUtils.isNotEmpty(request.getTransportMode())) {
                Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);
                predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(TRANSPORT_MODE), TransportMode.valueOf(request.getTransportMode()))));
            }

            if (StringUtils.isNotEmpty(request.getVehicleCountry())) {
                predicates.add(buildRegistrationCountrySubquery(request.getVehicleCountry(), cb, root));
            }
            predicates.add(buildIdentifierSubquery(request, cb, root));

            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    private Predicate buildRegistrationCountrySubquery(String registrationCountry, final CriteriaBuilder cb, final Root<Consignment> root) {
        Join<Consignment, MainCarriageTransportMovement> movementJoin = root.join(MOVEMENTS, JoinType.LEFT);
        Join<Consignment, UsedTransportEquipment> equipmentJoin = root.join(TRANSPORT_VEHICLES, JoinType.LEFT);
        return cb.or(cb.equal(movementJoin.get("usedTransportMeansRegistrationCountry"), registrationCountry),
                cb.equal(equipmentJoin.get(VEHICLE_COUNTRY), registrationCountry));
    }

    private Predicate buildIdentifierSubquery(final SearchWithIdentifiersRequestDto request, final CriteriaBuilder cb, final Root<Consignment> root) {
        // means, equipment, carried
        final List<Predicate> subQueryPredicate = new ArrayList<>();
        if (CollectionUtils.emptyIfNull(request.getIdentifierType()).isEmpty() || request.getIdentifierType().contains("means")) {
            final Join<Consignment, MainCarriageTransportMovement> movements = root.join(MOVEMENTS, JoinType.LEFT);
            subQueryPredicate.add(cb.equal(cb.upper(movements.get("usedTransportMeansId")), request.getVehicleID().toUpperCase()));
        }
        if (CollectionUtils.emptyIfNull(request.getIdentifierType()).isEmpty()
                || request.getIdentifierType().contains("equipment")
                || request.getIdentifierType().contains("carried")) {
            final Join<Consignment, UsedTransportEquipment> vehicles = root.join(TRANSPORT_VEHICLES, JoinType.LEFT);
            if (CollectionUtils.emptyIfNull(request.getIdentifierType()).isEmpty()
                    || request.getIdentifierType().contains("equipment")) {
                subQueryPredicate.add(cb.equal(cb.upper(vehicles.get(VEHICLE_ID)), request.getVehicleID().toUpperCase()));
            }
            if (CollectionUtils.emptyIfNull(request.getIdentifierType()).isEmpty()
                    || request.getIdentifierType().contains("carried")) {
                final Join<UsedTransportEquipment, CarriedTransportEquipment> carried = vehicles.join("carriedTransportEquipments", JoinType.LEFT);
                subQueryPredicate.add(cb.equal(cb.upper(carried.get(VEHICLE_ID)), request.getVehicleID().toUpperCase()));
            }
        }

        return cb.or(subQueryPredicate.toArray(new Predicate[]{}));
    }
}
