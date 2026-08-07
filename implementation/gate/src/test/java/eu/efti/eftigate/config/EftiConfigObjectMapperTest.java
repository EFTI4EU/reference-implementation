package eu.efti.eftigate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.eftigate.testsupport.IntegrationTest;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.OffsetDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EftiConfigObjectMapperTest extends IntegrationTest {

    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper objectMapper;

    @Test
    void objectMapperDoesNotAdjustOffsetDateTimeToContextTimeZone() throws Exception {
        ObjectMapper mapper = objectMapper.copy();
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));

        String json = "{\"eventDateTime\":\"2026-08-04T12:34:56+03:00\"}";
        OffsetPayload payload = mapper.readValue(json, OffsetPayload.class);

        assertEquals(OffsetDateTime.parse("2026-08-04T12:34:56+03:00"), payload.getEventDateTime());
    }

    @Setter
    @Getter
    private static class OffsetPayload {
        private OffsetDateTime eventDateTime;
    }
}
