package eu.efti.eftigate.service;

import eu.efti.commons.dto.UilDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UilDtoValidatorTest {

    @Test
    void shouldValidateAllFieldsEmpty() {
        final UilDto uilDto = UilDto.builder().build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<UilDto>> violations = validator.validate(uilDto);
        assertFalse(violations.isEmpty());
        assertEquals(3, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.GATE_ID_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.PLATFORM_ID_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.DATASET_ID_MISSING));
    }

    @Test
    void shouldValidateAllFieldsIncorrect() {
        final UilDto uilDto = UilDto.builder()
                .datasetId("abc-123")
                .platformId("pas une uri")
                .gateId("pas une uri")
                .build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<UilDto>> violations = validator.validate(uilDto);
        assertFalse(violations.isEmpty());
        assertEquals(3, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.GATE_ID_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.PLATFORM_ID_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.DATASET_ID_INCORRECT_FORMAT));

    }

    private boolean containsError(final Set<ConstraintViolation<UilDto>> violations, final ErrorCodesEnum error) {
        for (final ConstraintViolation<UilDto> violation : violations) {
            if (violation.getMessage().equals(error.name())) {
                return true;
            }
        }
        return false;
    }
}
