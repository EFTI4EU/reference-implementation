package eu.efti.eftigate.service;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchWithIdentifiersRequestDtoValidatorTest {

    @Test
    void shouldValidateAllFieldsEmpty() {
        final SearchWithIdentifiersRequestDto identifiersRequestDto = SearchWithIdentifiersRequestDto.builder().build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<SearchWithIdentifiersRequestDto>> violations = validator.validate(identifiersRequestDto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.IDENTIFIER_MISSING));
    }

    @Test
    void shouldValidateAllFieldsInvalid() {
        final SearchWithIdentifiersRequestDto identifiersRequestDto = SearchWithIdentifiersRequestDto.builder()
                .identifier("@aaa-123")
                .modeCode("#toto")
                .registrationCountryCode("truc")
                .eftiGateIndicator(List.of("tutu", "FR", "BE", "PP")).build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<SearchWithIdentifiersRequestDto>> violations = validator.validate(identifiersRequestDto);
        assertFalse(violations.isEmpty());
        assertEquals(5, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.IDENTIFIER_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.MODE_CODE_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.REGISTRATION_COUNTRY_INCORRECT));
        assertTrue(containsError(violations, ErrorCodesEnum.GATE_INDICATOR_INCORRECT));
    }

    private boolean containsError(final Set<ConstraintViolation<SearchWithIdentifiersRequestDto>> violations, final ErrorCodesEnum error) {
        for (final ConstraintViolation<SearchWithIdentifiersRequestDto> violation : violations) {
            if (violation.getMessage().equals(error.name())) {
                return true;
            }
        }
        return false;
    }

}
