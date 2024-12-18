package eu.efti.identifiersregistry.utils;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeDeserializer extends InstantDeserializer<OffsetDateTime> {
    public OffsetDateTimeDeserializer() {
        super(OffsetDateTime.class, DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                OffsetDateTime::from,
                fromMilliSeconds -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(fromMilliSeconds.value), fromMilliSeconds.zoneId),
                fromNanoSeconds -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(fromNanoSeconds.integer, fromNanoSeconds.fraction), fromNanoSeconds.zoneId),
                (offsetDateTime, zoneId) -> offsetDateTime.withOffsetSameInstant(zoneId.getRules().getOffset(offsetDateTime.toLocalDateTime())),
                true, JavaTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID.enabledByDefault(),
                JavaTimeFeature.ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS.enabledByDefault());
    }
}
