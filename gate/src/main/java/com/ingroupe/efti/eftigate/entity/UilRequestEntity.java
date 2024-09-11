package com.ingroupe.efti.eftigate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("UIL")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class UilRequestEntity extends RequestEntity{
    @Column(name = "reponsedata")
    private byte[] reponseData;
}
