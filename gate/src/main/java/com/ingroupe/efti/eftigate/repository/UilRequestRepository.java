package com.ingroupe.efti.eftigate.repository;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.eftigate.entity.UilRequestEntity;

public interface UilRequestRepository extends RequestRepository<UilRequestEntity> {
    UilRequestEntity findByControlRequestUuidAndStatus(final String EftiDataUuid, final RequestStatusEnum status);
}
