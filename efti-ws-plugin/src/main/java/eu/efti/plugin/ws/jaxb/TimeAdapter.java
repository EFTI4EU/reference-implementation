package eu.efti.plugin.ws.jaxb;

import eu.efti.commons.converter.StringToTemporalAccessorConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Custom adapter which extends {@link XmlAdapter} for {@code xsd:time} mapped to {@link LocalTime}
 */
@Slf4j
public class TimeAdapter extends XmlAdapter<String, LocalTime> {


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_TIME;

    private final StringToTemporalAccessorConverter converter;

    public TimeAdapter() {
        this.converter = new StringToTemporalAccessorConverter(FORMATTER);
    }

    @Override
    public LocalTime unmarshal(final String s) throws Exception {
        TemporalAccessor converted = converter.convert(s);
        if (!(converted instanceof LocalTime)) {
            log.warn("The source [{}] could not be correctly converted to a local time instance [{}]", s, converted);
            return null;
        }
        return (LocalTime) converter.convert(s);
    }

    @Override
    public String marshal(final LocalTime lt) {
        if (lt == null) {
            log.info("Returning null value for a null local time input");
            return null;
        }
        return lt.format(FORMATTER);
    }
}