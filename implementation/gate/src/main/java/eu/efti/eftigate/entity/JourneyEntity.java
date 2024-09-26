package eu.efti.eftigate.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.model.AbstractModel;
import eu.efti.identifiersregistry.utils.OffsetDateTimeDeserializer;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
public abstract class JourneyEntity implements Serializable {

    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime journeyStart;

    @Enumerated(EnumType.STRING)
    private CountryIndicator countryStart;

    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime journeyEnd;

    @Enumerated(EnumType.STRING)
    private CountryIndicator countryEnd;
}
