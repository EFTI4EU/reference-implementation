// UsedTransportEquipment.java
package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "used_transport_equipment")
public class UsedTransportEquipment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "sequence_number")
    private int sequenceNumber;

    @Column(name = "equipment_id")
    private String equipmentId;

    @Column(name = "id_scheme_agency_id")
    private String schemeAgencyId;

    @Column(name = "registration_country")
    private String registrationCountry;

    @Column(name = "category_code")
    private String categoryCode;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "consignment_id", referencedColumnName = "id", updatable = false)
    private Consignment consignment;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "usedTransportEquipment", targetEntity = CarriedTransportEquipment.class)
    @ToString.Exclude
    private List<CarriedTransportEquipment> carriedTransportEquipments = new ArrayList<>();

    public void setCarriedTransportEquipments(List<CarriedTransportEquipment> carriedTransportEquipments) {
        CollectionUtils.emptyIfNull(carriedTransportEquipments).forEach(eq -> eq.setUsedTransportEquipment(this));
        this.carriedTransportEquipments = carriedTransportEquipments;
    }
}
