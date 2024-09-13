package eu.efti.eftigate.repository;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.entity.UilRequestEntity;

public interface UilRequestRepository extends RequestRepository<UilRequestEntity> {
    UilRequestEntity findByControlRequestUuidAndStatus(final String EftiDataUuid, final RequestStatusEnum status);
}
