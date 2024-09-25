package eu.efti.eftigate.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Convert(attributeName = "entityAttrName", converter = JsonBinaryType.class)
@EqualsAndHashCode(callSuper = true)
public class IdentifiersRequestEntity extends RequestEntity {
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "identifiers")
    private IdentifiersResults identifiersResults;
}
