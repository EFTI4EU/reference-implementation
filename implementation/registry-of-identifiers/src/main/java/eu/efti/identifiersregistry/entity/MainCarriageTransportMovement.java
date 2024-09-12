package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class MainCarriageTransportMovement {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "consignmentId", column = @Column(name = "consignment_id")),
            @AttributeOverride(name = "ordinal", column = @Column(name = "ordinal"))
    })
    private MainCarriageTransportMovementId id;

    @Column(name = "mode_code")
    private short modeCode;

    @Column(name = "dangerous_goods_indicator")
    private boolean dangerousGoodsIndicator;

    @Column(name = "used_transport_means_id")
    private String usedTransportMeansId;

    @Column(name = "used_transport_means_registration_country")
    private String usedTransportMeansRegistrationCountry;

    @ManyToOne
    @JoinColumn(name = "consignment_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Consignment consignment;
}
