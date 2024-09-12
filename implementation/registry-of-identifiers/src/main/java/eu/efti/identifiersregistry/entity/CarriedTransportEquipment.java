package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class CarriedTransportEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "sequence_number")
    private int sequenceNumber;

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
