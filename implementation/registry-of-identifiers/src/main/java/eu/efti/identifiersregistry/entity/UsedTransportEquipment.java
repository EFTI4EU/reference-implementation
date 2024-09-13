// UsedTransportEquipment.java
package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "used_transport_equipment")
public class UsedTransportEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "sequence_number")
    private int sequenceNumber;

    @Column(name = "equipment_id")
    private String equipmentId;

    @Column(name = "id_scheme_agency_id")
    private String idSchemeAgencyId;

    @Column(name = "registration_country")
    private String registrationCountry;

    @ManyToOne
    @JoinColumn(name = "consignment_id", referencedColumnName = "id", insertable = true, updatable = false)
    private Consignment consignment;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "usedTransportEquipment", targetEntity = CarriedTransportEquipment.class)
    private List<CarriedTransportEquipment> carriedTransportEquipments;
}