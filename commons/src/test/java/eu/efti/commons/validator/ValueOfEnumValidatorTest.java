package eu.efti.commons.validator;

import eu.efti.commons.enums.CountryIndicator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Builder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueOfEnumValidatorTest {

    @Test
    void shouldValidate() {

        final TestObject testObject = TestObject.builder().country("DE").build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        assertTrue(validator.validate(testObject).isEmpty());
        assertEquals("DE", testObject.country);
    }

    @Test
    void shouldValidateIfEmpty() {

        final TestObject testObject = TestObject.builder().build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        assertTrue(validator.validate(testObject).isEmpty());
    }

    @Test
    void shouldSetError() {

        final TestObject testObject = TestObject.builder().country("toto").build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        assertEquals(1, validator.validate(testObject).size());
    }

    @Builder
    private static class TestObject {

        @ValueOfEnum(enumClass = CountryIndicator.class)
        private String country;
    }
}
