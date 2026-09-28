package eu.efti.eftigate.repository;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.entity.NoteRequestEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface NotesRequestRepository extends RequestRepository<NoteRequestEntity> {
    NoteRequestEntity findByStatusAndEdeliveryMessageId(RequestStatusEnum requestStatusEnum, String eDeliveryMessageId);
    NoteRequestEntity findByControlRequestIdAndStatus(String requestId, RequestStatusEnum status);
    NoteRequestEntity findByNoteRequestIdAndStatus(String noteRequestId, RequestStatusEnum status);
    NoteRequestEntity findByNoteRequestId(String noteRequestId);
    List<NoteRequestEntity> findByStatusInAndCreatedDateBefore(List<RequestStatusEnum> statuses, LocalDateTime cutoff);
}
