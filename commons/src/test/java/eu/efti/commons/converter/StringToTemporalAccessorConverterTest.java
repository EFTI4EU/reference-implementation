package eu.efti.commons.converter;

import eu.efti.commons.exception.EftiDateTimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.util.AssertionErrors.assertEquals;


class StringToTemporalAccessorConverterTest {

    private StringToTemporalAccessorConverter converter;

    @Test
    void convert_returnsNullWhenSourceIsNull() {
        // GIVEN
        String source = null;
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(source);

        // THEN
        assertNull(result, "Should have returned a null UTC local date time when the input source is null");
    }

    @Test
    void convert_throwsParseExceptionWhenDateTimeSourceHasWrongFormat() {
        // GIVEN
        String invalid = "2021/07/21 13:10:00";
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN AND THEN
        final Exception exception = assertThrows(EftiDateTimeException.class, () -> {
            converter.convert(invalid);
        });

        Assertions.assertEquals("Invalid date time value [" + invalid + "]", exception.getMessage());

    }

    @Test
    void convert_throwsParseExceptionWhenDateSourceHasWrongFormat() {
        // GIVEN
        String invalid = "2021/07/21";
        givenFormatter(DateTimeFormatter.ISO_DATE);

        // WHEN AND THEN
        final Exception exception = assertThrows(EftiDateTimeException.class, () -> {
            converter.convert(invalid);
        });
        Assertions.assertEquals("Invalid date time value [" + invalid + "]", exception.getMessage());

    }

    @Test
    void convert_throwsParseExceptionWhenTimeSourceHasWrongFormat() {
        // GIVEN
        String invalid = "13H10'00";
        givenFormatter(DateTimeFormatter.ISO_TIME);

        // WHEN AND THEN
        final Exception exception = assertThrows(EftiDateTimeException.class, () -> {
            converter.convert(invalid);
        });
        Assertions.assertEquals("Invalid date time value [" + invalid + "]", exception.getMessage());

    }

    @Test
    void convert_dateTimeSource() {
        // GIVEN
        String valid = "2021-07-21T10:15:30";
        LocalDateTime expected = LocalDateTime.of(2021, 7, 21, 10, 15, 30);
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        assertEquals("Should have returned a valid UTC local date time for a valid date time source", expected, result);
    }

    @Test
    void convert_dateTimeSourceWithOffset() {
        // GIVEN
        String valid = "2021-07-21T10:15:30+04:00";
        LocalDateTime expected = LocalDateTime.of(2021, 7, 21, 6, 15, 30);
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        assertEquals("Should have returned a valid UTC local date time for a valid offset date time source", expected, result);
    }

    @Test
    void convert_dateSource() {
        // GIVEN
        String valid = "2021-07-21";
        LocalDate expected = LocalDate.of(2021, 7, 21);
        givenFormatter(DateTimeFormatter.ISO_DATE);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        assertEquals("Should have returned a valid UTC local date for a valid date source", expected, result);
    }

    @Test
    void convert_dateSourceWithOffset_IgnoresOffset() {
        // GIVEN
        String valid = "2021-07-21+04:00";
        LocalDate expected = LocalDate.of(2021, 7, 21);
        givenFormatter(DateTimeFormatter.ISO_DATE);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        assertEquals("Should have ignored the offset and returned a valid UTC local date for a valid offset date source", expected, result);
    }

    @Test
    void convert_timeSource() {
        // GIVEN
        String valid = "10:15:30";
        LocalTime expected = LocalTime.of(10, 15, 30);
        givenFormatter(DateTimeFormatter.ISO_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        assertEquals("Should have returned a valid UTC local time for a valid time source", expected, result);
    }

    @Test
    void convert_timeSourceWithOffset() {
        // GIVEN
        String valid = "10:15:30+04:30";
        LocalTime expected = LocalTime.of(5, 45, 30);
        givenFormatter(DateTimeFormatter.ISO_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        assertEquals("Should have returned a valid UTC local time for a valid offset time source", expected, result);
    }

    public void givenFormatter(final DateTimeFormatter formatter) {
        converter = new StringToTemporalAccessorConverter(formatter);
    }
}