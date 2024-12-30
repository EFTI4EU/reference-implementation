package eu.efti.plugin.ws.jaxb;

import eu.efti.commons.converter.StringToTemporalAccessorConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Custom adapter which extends {@link XmlAdapter} for {@code xsd:date} mapped to {@link LocalDate}
 */
@Slf4j
@Component
public class DateAdapter extends XmlAdapter<String, LocalDate> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;

    private final StringToTemporalAccessorConverter converter;

    public DateAdapter() {
        this.converter = new StringToTemporalAccessorConverter(FORMATTER);
    }

    @Override
    public LocalDate unmarshal(String s) {
        //this is mapped to xsd:date with or without timezone
        TemporalAccessor converted = converter.convert(s);
        if (!(converted instanceof LocalDate)) {
            log.warn("The source [{}] could not be correctly converted to a local date instance [{}]", s, converted);
            return null;
        }
        return (LocalDate) converted;
    }

    @Override
    public String marshal(LocalDate dt) {
        if (dt == null) {
            log.info("Returning null value for a null local date input");
            return null;
        }
        return dt.format(FORMATTER);
    }

}