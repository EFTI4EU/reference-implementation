package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "carried_transport_equipment")
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

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "used_transport_equipment_id", referencedColumnName = "id", insertable = true, updatable = false)
    private UsedTransportEquipment usedTransportEquipment;
}
