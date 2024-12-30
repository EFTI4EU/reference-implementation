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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(JMockitExtension.class)
class DateAdapterTest {

    @Injectable
    private StringToTemporalAccessorConverter converter;

    @Tested(availableDuringSetup = true)
    private DateAdapter dateAdapter;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(dateAdapter, "converter", converter, true);
    }

    @Test
    void testUnmarshall_returnsNullDateForNullInputString() {
        // GIVEN
        String input = null;
        new Expectations() {{
            converter.convert(input);
            result = null;
        }};

        // WHEN
        LocalDate result = dateAdapter.unmarshal(input);

        // THEN
        assertNull(result, "Should have returned null when unmarshalling a null input string");
    }

    @Test
    void testUnmarshall_returnsParsedDateForNonNullInputString(@Injectable LocalDate parsedDate) {
        // GIVEN
        String input = "2019-04-17";
        new Expectations() {{
            converter.convert(input);
            result = parsedDate;
        }};

        // WHEN
        LocalDate result = dateAdapter.unmarshal(input);

        // THEN
        assertSame(parsedDate, result, "Should have returned the parsed date when unmarshalling a non-null input string");
    }

    @Test
    void testMarshal_returnsNullFormattedDateForNullInputDate() {
        // GIVEN
        LocalDate input = null;

        // WHEN
        String result = dateAdapter.marshal(input);

        // THEN
        assertNull(result, "Should have returned null when unmarshalling a null input date");
    }


    @Test
    void testMarshall_returnsFormattedDateForNonNullInputDate(@Injectable LocalDate inputDate) {
        // GIVEN
        String formattedDate = "2019-04-17";
        new Expectations() {{
            inputDate.format(DateTimeFormatter.ISO_DATE);
            result = formattedDate;
        }};

        // WHEN
        String result = dateAdapter.marshal(inputDate);

        // THEN
        assertEquals(formattedDate, result, "Should have returned the formatted date when marshalling a non-null input date");
    }
}