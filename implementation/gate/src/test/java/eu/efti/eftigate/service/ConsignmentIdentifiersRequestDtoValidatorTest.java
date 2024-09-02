package eu.efti.eftigate.service;

import eu.efti.commons.dto.ConsignmentIdentifiersRequestDto;
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

class ConsignmentIdentifiersRequestDtoValidatorTest {

    @Test
    void shouldValidateAllFieldsEmpty() {
        final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto = ConsignmentIdentifiersRequestDto.builder().build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<ConsignmentIdentifiersRequestDto>> violations = validator.validate(consignmentIdentifiersRequestDto);
        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.VEHICLE_ID_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_MISSING));
    }

    @Test
    void shouldValidateAllFieldsInvalid() {
        final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto = ConsignmentIdentifiersRequestDto.builder()
                .vehicleID("aaa-123")
                .transportMode("toto")
                .vehicleCountry("truc")
                .eFTIGateIndicator(List.of("tutu", "FR", "BE", "PP")).build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<ConsignmentIdentifiersRequestDto>> violations = validator.validate(consignmentIdentifiersRequestDto);
        assertFalse(violations.isEmpty());
        assertEquals(8, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.VEHICLE_ID_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.TRANSPORT_MODE_INCORRECT));
        assertTrue(containsError(violations, ErrorCodesEnum.VEHICLE_COUNTRY_INCORRECT));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.GATE_INDICATOR_INCORRECT));
    }

    private boolean containsError(final Set<ConstraintViolation<ConsignmentIdentifiersRequestDto>> violations, final ErrorCodesEnum error) {
        for(final ConstraintViolation<ConsignmentIdentifiersRequestDto> violation : violations ) {
            if(violation.getMessage().equals(error.name())) {
                return true;
            }
        }
        return false;
    }

}
