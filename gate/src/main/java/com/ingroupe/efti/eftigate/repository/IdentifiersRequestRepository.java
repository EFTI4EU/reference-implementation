package com.ingroupe.efti.eftigate.repository;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.eftigate.entity.IdentifiersRequestEntity;

public interface IdentifiersRequestRepository extends RequestRepository<IdentifiersRequestEntity> {
   IdentifiersRequestEntity findByControlRequestUuidAndStatusAndGateUrlDest(String requestUuid, RequestStatusEnum requestStatusEnum, String gateUrlDest);
}
