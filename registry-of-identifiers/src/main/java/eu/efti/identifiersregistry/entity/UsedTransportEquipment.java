// UsedTransportEquipment.java
package eu.efti.identifiersregistry.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private String idSchemeAgencyId;

    @Column(name = "registration_country")
    private String registrationCountry;

    @Column(name = "category_code")
    private String categoryCode;

    @ManyToOne
    @JoinColumn(name = "consignment_id", referencedColumnName = "id", insertable = true, updatable = false)
    private Consignment consignment;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "usedTransportEquipment", targetEntity = CarriedTransportEquipment.class)
    private List<CarriedTransportEquipment> carriedTransportEquipments = new ArrayList<>();

    public void setCarriedTransportEquipments(List<CarriedTransportEquipment> carriedTransportEquipments) {
        CollectionUtils.emptyIfNull(carriedTransportEquipments).forEach(eq -> eq.setUsedTransportEquipment(this));
        this.carriedTransportEquipments = carriedTransportEquipments;
    }
}
