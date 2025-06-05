package eu.efti.eftigate.entity;


import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.model.AbstractModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "gate", catalog = "efti")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GateEntity extends AbstractModel implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private CountryIndicator country;

    @Column(name = "gateid")
    private String gateId;
}
