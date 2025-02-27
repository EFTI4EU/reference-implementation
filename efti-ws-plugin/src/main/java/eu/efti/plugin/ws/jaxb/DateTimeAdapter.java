package eu.efti.plugin.ws.jaxb;


import eu.efti.commons.converter.StringToTemporalAccessorConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Custom adapter which extends {@link XmlAdapter} for {@code xsd:dateTime} mapped to {@code LocalDateTime}
 */
@Slf4j
public class DateTimeAdapter extends XmlAdapter<String, LocalDateTime> {


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final StringToTemporalAccessorConverter converter;

    public DateTimeAdapter() {
        this.converter = new StringToTemporalAccessorConverter(FORMATTER);
    }

    @Override
    public LocalDateTime unmarshal(final String s) {
        TemporalAccessor converted = converter.convert(s);
        if (!(converted instanceof LocalDateTime)) {
            log.warn("The source [{}] could not be correctly converted to a local date time instance [{}]", s, converted);
            return null;
        }
        return (LocalDateTime) converter.convert(s);
    }

    @Override
    public String marshal(final LocalDateTime dt) {
        if (dt == null) {
            log.info("Returning null value for a null local date time input");
            return null;
        }
        return dt.format(FORMATTER);
    }
}