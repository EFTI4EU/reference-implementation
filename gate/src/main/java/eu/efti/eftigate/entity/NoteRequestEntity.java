package eu.efti.eftigate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Entity
@DiscriminatorValue("NOTE")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NoteRequestEntity extends RequestEntity {

    @Serial
    private static final long serialVersionUID = 1130386355719585259L;

    @Column(name = "note")
    private String note;
}
