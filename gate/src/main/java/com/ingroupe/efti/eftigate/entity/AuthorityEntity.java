package com.ingroupe.efti.eftigate.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "authority", catalog = "efti")
@Getter
@Setter
public class AuthorityEntity implements Serializable {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    
    @Column(name = "country")
    private String country;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "legalcontact", referencedColumnName = "id")
    private ContactInformationEntity legalContact;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "workingcontact", referencedColumnName = "id")
    private ContactInformationEntity workingContact;
    
    @Column(name = "isemergencyservice")
    private boolean isEmergencyService;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "nationaluniqueidentifier")
    private String nationalUniqueIdentifier;
}
