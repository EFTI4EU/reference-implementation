package eu.efti.eftigate.repository;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.entity.ControlEntity;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ControlRepository extends JpaRepository<ControlEntity, Long>, JpaSpecificationExecutor<ControlEntity> {
    @Transactional("controlTransactionManager")
    Optional<ControlEntity> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    default List<ControlEntity> findByCriteria(final StatusEnum status, final Integer timeoutValue) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), status));
            final Path<LocalDateTime> dateCreatedPath = root.get("createdDate");
            predicates.add(cb.lessThan(dateCreatedPath, LocalDateTime.now().minusSeconds(timeoutValue)));
            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }

    default List<ControlEntity> findByCriteria(final String requestId, final RequestStatusEnum requestStatus) {
        return this.findAll((root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("requestId"), requestId));
            predicates.add(cb.equal(root.join("requests").get("status"), requestStatus));
            return cb.and(predicates.toArray(new Predicate[]{}));
        });
    }
}
