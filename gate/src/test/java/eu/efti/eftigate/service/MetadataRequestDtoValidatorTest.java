package eu.efti.eftigate.service;

import eu.efti.commons.dto.MetadataRequestDto;
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

class MetadataRequestDtoValidatorTest {

    @Test
    void shouldValidateAllFieldsEmpty() {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder().build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<MetadataRequestDto>> violations = validator.validate(metadataRequestDto);
        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.VEHICLE_ID_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_MISSING));
    }

    @Test
    void shouldValidateAllFieldsInvalid() {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder()
                .vehicleID("aaa-123")
                .transportMode("toto")
                .vehicleCountry("truc")
                .eFTIGateIndicator(List.of("tutu", "FR", "BE", "PP")).build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<MetadataRequestDto>> violations = validator.validate(metadataRequestDto);
        assertFalse(violations.isEmpty());
        assertEquals(8, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.VEHICLE_ID_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.TRANSPORT_MODE_INCORRECT));
        assertTrue(containsError(violations, ErrorCodesEnum.VEHICLE_COUNTRY_INCORRECT));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.GATE_INDICATOR_INCORRECT));
    }

    private boolean containsError(final Set<ConstraintViolation<MetadataRequestDto>> violations, final ErrorCodesEnum error) {
        for(final ConstraintViolation<MetadataRequestDto> violation : violations ) {
            if(violation.getMessage().equals(error.name())) {
                return true;
            }
        }
        return false;
    }

}
