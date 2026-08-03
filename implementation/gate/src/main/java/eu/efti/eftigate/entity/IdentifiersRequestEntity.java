package eu.efti.eftigate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@DiscriminatorValue("IDENTIFIER")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class IdentifiersRequestEntity extends RequestEntity {
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "identifiers")
    private IdentifiersResults identifiersResults;
}
