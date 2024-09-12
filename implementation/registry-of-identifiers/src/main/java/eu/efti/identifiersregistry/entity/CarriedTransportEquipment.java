package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class CarriedTransportEquipment {
    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "consignmentId", column = @Column(name = "consignment_id")),
            @AttributeOverride(name = "transportEquipmentSequenceNumber", column = @Column(name = "transport_equipment_sequence_number")),
            @AttributeOverride(name = "sequenceNumber", column = @Column(name = "sequence_number"))
    })
    private CarriedTransportEquipmentId id;

    @Column(name = "equipment_id")
    private String equipmentId;
    @Column(name = "id_scheme_agency_id")
    private String schemeAgencyId;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "consignment_id", referencedColumnName = "consignment_id", insertable = false, updatable = false),
            @JoinColumn(name = "transport_equipment_sequence_number", referencedColumnName = "sequence_number", insertable = false, updatable = false)
    })
    private UsedTransportEquipment usedTransportEquipment;
}
