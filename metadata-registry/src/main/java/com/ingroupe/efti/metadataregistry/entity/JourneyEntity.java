package com.ingroupe.efti.metadataregistry.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.commons.model.AbstractModel;
import com.ingroupe.efti.metadataregistry.utils.OffsetDateTimeDeserializer;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@Data
public abstract class JourneyEntity extends AbstractModel implements Serializable {

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
