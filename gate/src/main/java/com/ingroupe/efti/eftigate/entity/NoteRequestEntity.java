package com.ingroupe.efti.eftigate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("NOTE")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NoteRequestEntity extends RequestEntity{
    @Column(name = "note")
    private String note;
}
