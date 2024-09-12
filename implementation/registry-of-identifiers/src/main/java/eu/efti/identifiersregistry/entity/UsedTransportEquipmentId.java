package eu.efti.identifiersregistry.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class UsedTransportEquipmentId implements Serializable {
    private long consignmentId;
    private int sequenceNumber;
}