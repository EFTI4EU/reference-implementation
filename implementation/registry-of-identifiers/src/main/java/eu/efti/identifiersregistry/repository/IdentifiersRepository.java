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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

    @Query(value = "SELECT c FROM Consignment c where c.gateId = :gate and c.datasetId = :uuid and c.platformId = :platform")
    Optional<Consignment> findByUil(final String gate, final String uuid, final String platform);

    default List<Consignment> searchByCriteria(final SearchWithIdentifiersRequestDto request) {
        final Set<Consignment> results = new HashSet<>();
        List<String> identifierTypes = request.getIdentifierType();
        if (CollectionUtils.isNotEmpty(identifierTypes)) {
            identifierTypes.forEach(identifierType -> {
                if (MEANS.equalsIgnoreCase(identifierType)) {
                    results.addAll(findAllForMeans(request));
                } else if (EQUIPMENT.equalsIgnoreCase(identifierType)) {
                    results.addAll(findAllForEquipment(request));
                } else if (CARRIED.equalsIgnoreCase(identifierType)) {
                    results.addAll(findAllForCarried(request));
                }
            });
        } else {
            results.addAll(Stream.of(findAllForMeans(request), findAllForEquipment(request), findAllForCarried(request))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet()));
        }
        return new ArrayList<>(results);
    }

    default List<Consignment> findAllForMeans(SearchWithIdentifiersRequestDto request) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);

            predicates.add(cb.equal(cb.upper(mainCarriageTransportMovementJoin.get(USED_TRANSPORT_MEANS_ID)), request.getIdentifier().toUpperCase()));

            buildCommonAttributesRequest(request, cb, predicates, mainCarriageTransportMovementJoin);

            if (StringUtils.isNotBlank(request.getRegistrationCountryCode())) {
                predicates.add(cb.equal(mainCarriageTransportMovementJoin.get(USED_TRANSPORT_MEANS_REGISTRATION_COUNTRY), request.getRegistrationCountryCode()));
            }

            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    default List<Consignment> findAllForEquipment(SearchWithIdentifiersRequestDto request) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);
            Join<Consignment, UsedTransportEquipment> equipmentJoin = root.join(TRANSPORT_VEHICLES, JoinType.LEFT);
            predicates.add(cb.equal(cb.upper(equipmentJoin.get(VEHICLE_ID)), request.getIdentifier().toUpperCase()));

            buildCommonAttributesRequest(request, cb, predicates, mainCarriageTransportMovementJoin);

            if (StringUtils.isNotBlank(request.getRegistrationCountryCode())) {
                predicates.add(cb.equal(equipmentJoin.get(VEHICLE_COUNTRY), request.getRegistrationCountryCode()));
            }
            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    default List<Consignment> findAllForCarried(SearchWithIdentifiersRequestDto request) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin = root.join(MOVEMENTS, JoinType.LEFT);
            Join<Consignment, UsedTransportEquipment> equipmentJoin = root.join(TRANSPORT_VEHICLES, JoinType.LEFT);
            Join<UsedTransportEquipment, CarriedTransportEquipment> carriedJoin = equipmentJoin.join(CARRIED_TRANSPORT_EQUIPMENTS, JoinType.LEFT);

            predicates.add(cb.equal(cb.upper(carriedJoin.get(VEHICLE_ID)), request.getIdentifier().toUpperCase()));

            buildCommonAttributesRequest(request, cb, predicates, mainCarriageTransportMovementJoin);

            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    private void buildCommonAttributesRequest(SearchWithIdentifiersRequestDto request, CriteriaBuilder cb, List<Predicate> predicates, Join<Consignment, MainCarriageTransportMovement> mainCarriageTransportMovementJoin) {
        if (request.getDangerousGoodsIndicator() != null) {
            predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(IS_DANGEROUS_GOODS), request.getDangerousGoodsIndicator())));
        }
        if (StringUtils.isNotBlank(request.getModeCode())) {
            predicates.add(cb.and(cb.equal(mainCarriageTransportMovementJoin.get(TRANSPORT_MODE), request.getModeCode())));
        }
    }
}
