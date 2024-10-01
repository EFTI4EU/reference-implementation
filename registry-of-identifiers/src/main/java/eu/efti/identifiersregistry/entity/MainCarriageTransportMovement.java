package eu.efti.identifiersregistry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "main_carriage_transport_movement")
public class MainCarriageTransportMovement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "mode_code")
    private short modeCode;

    @Column(name = "dangerous_goods_indicator")
    private boolean dangerousGoodsIndicator;

    @Column(name = "used_transport_means_id")
    private String usedTransportMeansId;

    @Column(name = "used_transport_means_registration_country")
    private String usedTransportMeansRegistrationCountry;

    @ManyToOne
    @JoinColumn(name = "consignment_id", referencedColumnName = "id", insertable = true, updatable = false)
    private Consignment consignment;
}
