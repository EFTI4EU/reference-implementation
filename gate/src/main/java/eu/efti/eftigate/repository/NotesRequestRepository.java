package eu.efti.eftigate.repository;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.entity.NoteRequestEntity;

public interface NotesRequestRepository extends RequestRepository<NoteRequestEntity> {
    NoteRequestEntity findByStatusAndEdeliveryMessageId(RequestStatusEnum requestStatusEnum, String eDeliveryMessageId);
}
