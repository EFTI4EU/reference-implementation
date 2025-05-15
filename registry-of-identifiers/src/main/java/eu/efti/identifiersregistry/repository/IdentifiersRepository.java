package eu.efti.identifiersregistry.repository;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IdentifiersRepository extends JpaRepository<Consignment, Long>, JpaSpecificationExecutor<Consignment> {

    String VEHICLE_COUNTRY = "registrationCountry";
    String TRANSPORT_MODE = "modeCode";
    String IS_DANGEROUS_GOODS = "dangerousGoodsIndicator";
    String MOVEMENTS = "mainCarriageTransportMovements";
    String TRANSPORT_VEHICLES = "usedTransportEquipments";
    String VEHICLE_ID = "equipmentId";
    String EQUIPMENT = "equipment";
    String CARRIED = "carried";
    String MEANS = "means";
    String CARRIED_TRANSPORT_EQUIPMENTS = "carriedTransportEquipments";
    String USED_TRANSPORT_MEANS_REGISTRATION_COUNTRY = "usedTransportMeansRegistrationCountry";
    String USED_TRANSPORT_MEANS_ID = "usedTransportMeansId";

    @Query(value = "SELECT c FROM Consignment c where c.gateId = :gate and c.datasetId = :datasetId and c.platformId = :platform and CURRENT_TIMESTAMP <= c.disabledDate")
    Optional<Consignment> findActiveByUil(final String gate, final String datasetId, final String platform);

    @Query(value = "SELECT c FROM Consignment c where c.gateId = :gate and c.datasetId = :datasetId and c.platformId = :platform")
    Optional<Consignment> findByUil(final String gate, final String datasetId, final String platform);

    @Modifying
    @Query(value = "DELETE from Consignment c where c.disabledDate <= current_timestamp")
    int deleteAllDisabledConsignment();

    default List<Consignment> searchByCriteria(final SearchWithIdentifiersRequestDto request, final boolean isActivated) {
        final Set<Consignment> results = new HashSet<>();
        List<String> identifierTypes = request.getIdentifierType();
        if (CollectionUtils.isNotEmpty(identifierTypes)) {
            identifierTypes.forEach(identifierType -> {
                if (MEANS.equalsIgnoreCase(identifierType)) {
                    results.addAll(findAllForMeans(request, isActivated));
                } else if (EQUIPMENT.equalsIgnoreCase(identifierType)) {
                    results.addAll(findAllForEquipment(request, isActivated));
                } else if (CARRIED.equalsIgnoreCase(identifierType)) {
                    results.addAll(findAllForCarried(request, isActivated));
                }
            });
        } else {
            results.addAll(Stream.of(findAllForMeans(request, isActivated), findAllForEquipment(request, isActivated), findAllForCarried(request, isActivated))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet()));
        }
        return new ArrayList<>(results);
    }

    default List<Consignment> findAllForMeans(final SearchWithIdentifiersRequestDto request, final boolean isActivated) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);

            predicates.add(cb.equal(cb.upper(mainCarriageTransportMovementJoin.get(USED_TRANSPORT_MEANS_ID)), request.getIdentifier().toUpperCase()));

            buildCommonAttributesRequest(request, cb, predicates, mainCarriageTransportMovementJoin, root, isActivated);

            if (StringUtils.isNotBlank(request.getRegistrationCountryCode())) {
                predicates.add(cb.equal(mainCarriageTransportMovementJoin.get(USED_TRANSPORT_MEANS_REGISTRATION_COUNTRY), request.getRegistrationCountryCode()));
            }

            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    default List<Consignment> findAllForEquipment(final SearchWithIdentifiersRequestDto request, final boolean isActivated) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);
            Join<Consignment, UsedTransportEquipment> equipmentJoin = root.join(TRANSPORT_VEHICLES, JoinType.LEFT);
            predicates.add(cb.equal(cb.upper(equipmentJoin.get(VEHICLE_ID)), request.getIdentifier().toUpperCase()));

            buildCommonAttributesRequest(request, cb, predicates, mainCarriageTransportMovementJoin, root, isActivated);

            if (StringUtils.isNotBlank(request.getRegistrationCountryCode())) {
                predicates.add(cb.equal(equipmentJoin.get(VEHICLE_COUNTRY), request.getRegistrationCountryCode()));
            }
            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    default List<Consignment> findAllForCarried(final SearchWithIdentifiersRequestDto request, final boolean isActivated) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);
            Join<Consignment, UsedTransportEquipment> equipmentJoin = root.join(TRANSPORT_VEHICLES, JoinType.LEFT);
            Join<UsedTransportEquipment, CarriedTransportEquipment> carriedJoin = equipmentJoin.join(CARRIED_TRANSPORT_EQUIPMENTS, JoinType.LEFT);

            predicates.add(cb.equal(cb.upper(carriedJoin.get(VEHICLE_ID)), request.getIdentifier().toUpperCase()));

            buildCommonAttributesRequest(request, cb, predicates, mainCarriageTransportMovementJoin, root, isActivated);

            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    private void buildCommonAttributesRequest(final SearchWithIdentifiersRequestDto request, final CriteriaBuilder cb, final List<Predicate> predicates, final Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin, final Root<Consignment> root, final boolean isActivated) {
        if (request.getDangerousGoodsIndicator() != null) {
            predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(IS_DANGEROUS_GOODS), request.getDangerousGoodsIndicator())));
        }
        if (StringUtils.isNotBlank(request.getModeCode())) {
            predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(TRANSPORT_MODE), request.getModeCode())));
        }
        if (isActivated) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("disabledDate").as(OffsetDateTime.class), OffsetDateTime.now(Clock.systemUTC())));
        }
    }
}
