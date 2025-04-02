package eu.efti.eftigate.repository;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;

public interface IdentifiersRequestRepository extends RequestRepository<IdentifiersRequestEntity> {
    IdentifiersRequestEntity findByControlRequestIdAndStatusAndGateIdDest(String requestId, RequestStatusEnum requestStatusEnum, String gateIdDest);

    IdentifiersRequestEntity findByControlRequestIdAndGateIdDest(String requestId, String gateIdDest);
}
