package eu.efti.commons.converter;

import eu.efti.commons.exception.EftiDateTimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;


@Slf4j
public class StringToTemporalAccessorConverter implements Converter<String, TemporalAccessor> {

    private final DateTimeFormatter formatter;

    public StringToTemporalAccessorConverter(final DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * Converts a source input date-time, date or time, with or without a timezone to a UTC temporal.
     *
     * @param source the input date-time, date or time, with or without a timezone and/or a timezone offset.
     * @return a temporal in the UTC timezone.
     */
    @Override
    public TemporalAccessor convert(@Nullable final String source) {
        if (source == null) {
            log.info("Returning null temporal for null input string");
            return null;
        }
        TemporalAccessor result;
        try {
            result = formatter.parseBest(source,
                    OffsetDateTime::from, OffsetTime::from, LocalDateTime::from, LocalDate::from, LocalTime::from);
            if (result instanceof OffsetDateTime offsetDateTime) {
                log.debug("Unmarshalling an offset date time");
                result = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } else if (result instanceof OffsetTime offsetTime) {
                log.debug("Unmarshalling a local date time with timezone offset");
                result = offsetTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime();
            } else if (result instanceof LocalDateTime) {
                log.debug("Unmarshalling a local date time without timezone offset");
            } else if (result instanceof LocalDate) {
                log.debug("Unmarshalling a local date with or without timezone offset");
            } else if (result instanceof LocalTime) {
                log.debug("Unmarshalling a local time without zone offset");
            }
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new EftiDateTimeException("Invalid date time value [" + source + "]", exception);
        }

        log.info("Returning temporal [{}]", result);
        return result;
    }
}
