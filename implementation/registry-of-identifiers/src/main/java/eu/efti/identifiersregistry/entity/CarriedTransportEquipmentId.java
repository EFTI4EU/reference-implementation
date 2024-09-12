package eu.efti.identifiersregistry.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class CarriedTransportEquipmentId {
    private long consignmentId;
    private int transportEquipmentSequenceNumber;
    private int sequenceNumber;
}
