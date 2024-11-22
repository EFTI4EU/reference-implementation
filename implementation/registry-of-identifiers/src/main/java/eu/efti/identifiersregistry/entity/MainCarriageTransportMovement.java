package eu.efti.identifiersregistry.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "main_carriage_transport_movement")
public class MainCarriageTransportMovement {

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

    @ToString.Exclude
    @Column(name = "id_scheme_agency_id")
    private String schemeAgencyId;

    @ManyToOne
    @JoinColumn(name = "consignment_id", referencedColumnName = "id", updatable = false)
    private Consignment consignment;
}
