package eu.efti.plugin.ws.jaxb;

import eu.efti.commons.converter.StringToTemporalAccessorConverter;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


@ExtendWith(JMockitExtension.class)
class DateTimeAdapterTest {

    @Injectable
    private StringToTemporalAccessorConverter converter;

    @Tested(availableDuringSetup = true)
    private DateTimeAdapter dateTimeAdapter;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(dateTimeAdapter, "converter", converter, true);
    }

    @Test
    void testUnmarshall_returnsNullDateTimeForNullInputString() throws Exception {
        // GIVEN
        String input = null;
        new Expectations() {{
            converter.convert(input);
            result = null;
        }};

        // WHEN
        LocalDateTime result = dateTimeAdapter.unmarshal(input);

        // THEN
        assertNull(result, "Should have returned null when unmarshalling a null input string");
    }

    @Test
    void testUnmarshall_returnsParsedDateTimeForNonNullInputString(@Injectable LocalDateTime parsedDateTime) throws Exception {
        // GIVEN
        String input = "2019-04-17T09:34:36";
        new Expectations() {{
            converter.convert(input);
            result = parsedDateTime;
        }};

        // WHEN
        LocalDateTime result = dateTimeAdapter.unmarshal(input);

        // THEN
        assertSame(parsedDateTime, result, "Should have returned the parsed date time when unmarshalling a non-null input string");
    }

    @Test
    void testMarshal_returnsNullFormattedDateTimeForNullInputDateTime() {
        // GIVEN
        LocalDateTime input = null;

        // WHEN
        String result = dateTimeAdapter.marshal(input);

        // THEN
        assertNull(result, "Should have returned null when marshalling a null input date time");
    }


    @Test
    void testMarshall_returnsFormattedDateTimeForNonNullInputDateTime(@Injectable LocalDateTime inputDate) throws Exception {
        // GIVEN
        String formattedDateTime = "2019-04-17T09:34:36";
        new Expectations() {{
            inputDate.format(DateTimeFormatter.ISO_DATE_TIME);
            result = formattedDateTime;
        }};

        // WHEN
        String result = dateTimeAdapter.marshal(inputDate);

        // THEN
        assertEquals(formattedDateTime, result, "Should have returned the formatted date time when marshalling a non-null input date time");
    }
}