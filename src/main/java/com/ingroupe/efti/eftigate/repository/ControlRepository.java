package com.ingroupe.efti.eftigate.repository;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ControlRepository extends JpaRepository<ControlEntity, Long>, JpaSpecificationExecutor<ControlEntity> {
    Optional<ControlEntity> findByRequestUuid(String requestUuid);

    default List<ControlEntity> findByCriteria(final StatusEnum status, final Integer timeoutValue){
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), status));
            final Path<LocalDateTime> dateCreatedPath = root.get("createdDate");
            predicates.add(cb.lessThan(dateCreatedPath, LocalDateTime.now().minusSeconds(timeoutValue)));
            return cb.and(predicates.toArray(new Predicate[] {}));
        });
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    default List<ControlEntity> findByCriteria(final String requestUuid, final RequestStatusEnum requestStatus) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("requestUuid"), requestUuid));
            predicates.add(cb.equal(root.join("requests").get("status"), requestStatus));
            return cb.and(predicates.toArray(new Predicate[] {}));
        });
    }
}
