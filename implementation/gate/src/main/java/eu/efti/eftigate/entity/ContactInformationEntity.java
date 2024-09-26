package eu.efti.eftigate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "contactinformation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInformationEntity implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "email")
    private String email;

    @Column(name = "streetname")
    private String streetName;

    @Column(name = "buildingnumber")
    private String buildingNumber;

    @Column(name = "city")
    private String city;

    @Column(name = "additionalline")
    private String additionalLine;

    @Column(name = "postalcode")
    private String postalCode;
}
