package com.ingroupe.efti.eftigate.repository;

import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.eftigate.entity.NoteRequestEntity;

public interface NotesRequestRepository extends RequestRepository<NoteRequestEntity> {
    NoteRequestEntity findByStatusAndEdeliveryMessageId(RequestStatusEnum requestStatusEnum, String eDeliveryMessageId);
}
