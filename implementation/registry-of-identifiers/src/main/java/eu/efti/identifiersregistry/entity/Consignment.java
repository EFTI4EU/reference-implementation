package eu.efti.identifiersregistry.entity;

import eu.efti.commons.model.AbstractModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "consignment")
public class Consignment extends AbstractModel implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private long id;
    @Column(name = "platform_id")
    private String platformId;
    @Column(name = "dataset_id")
    private String datasetId;
    @Column(name = "gate_id")
    private String gateId;
    @Column(name = "carrier_acceptance_datetime")
    private OffsetDateTime carrierAcceptanceDatetime;
    @Column(name = "delivery_event_actual_occurrence_datetime")
    private OffsetDateTime deliveryEventActualOccurrenceDatetime;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "consignment")
    @Builder.Default
    @ToString.Exclude
    private List<MainCarriageTransportMovement> mainCarriageTransportMovements = new ArrayList<>();

    public void setMainCarriageTransportMovements(List<MainCarriageTransportMovement> mainCarriageTransportMovements) {
        this.mainCarriageTransportMovements.forEach(mctm -> mctm.setConsignment(null));
        this.mainCarriageTransportMovements.clear();
        if (mainCarriageTransportMovements != null) {
            this.mainCarriageTransportMovements.addAll(mainCarriageTransportMovements);
            this.mainCarriageTransportMovements.forEach(mctm -> mctm.setConsignment(this));
        }
    }

    @Builder.Default
    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "consignment")
    private List<UsedTransportEquipment> usedTransportEquipments = new ArrayList<>();

    public void setUsedTransportEquipments(List<UsedTransportEquipment> usedTransportEquipments) {
        this.usedTransportEquipments.forEach(ute -> ute.setConsignment(null));
        this.usedTransportEquipments.clear();
        if (usedTransportEquipments != null) {
            this.usedTransportEquipments.addAll(usedTransportEquipments);
            this.usedTransportEquipments.forEach(ute -> ute.setConsignment(this));
        }
    }
}
