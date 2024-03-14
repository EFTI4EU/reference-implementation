package com.ingroupe.efti.metadataregistry.entity;

import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.commons.enums.TransportMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transportvehicle")
public class TransportVehicle extends JourneyEntity implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Enumerated(EnumType.STRING)
    private TransportMode transportMode;
    private int sequence;
    private String vehicleId;
    @Enumerated(EnumType.STRING)
    private CountryIndicator vehicleCountry;
    @Enumerated(EnumType.STRING)
    private CountryIndicator countryEnd;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="metadata")
    MetadataEntity metadata;
}