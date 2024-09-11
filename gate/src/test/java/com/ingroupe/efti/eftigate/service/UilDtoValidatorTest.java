package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.dto.AuthorityDto;
import com.ingroupe.efti.commons.dto.ContactInformationDto;
import com.ingroupe.efti.commons.dto.UilDto;
import com.ingroupe.efti.commons.enums.ErrorCodesEnum;
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
        assertEquals(7, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.UIL_GATE_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.UIL_PLATFORM_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.UIL_UUID_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_MISSING));
    }

    @Test
    void shouldValidateAllFieldsIncorrect() {
        final UilDto uilDto = UilDto.builder()
                .eFTIDataUuid("abc-123")
                .eFTIPlatformUrl("pas une uri")
                .eFTIGateUrl("pas une uri")
                .authority(AuthorityDto.builder().build()).build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<UilDto>> violations = validator.validate(uilDto);
        assertFalse(violations.isEmpty());
        assertEquals(9, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.UIL_GATE_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.UIL_PLATFORM_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.UIL_UUID_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_LEGAL_CONTACT_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_WORKING_CONTACT_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_NATIONAL_IDENTIFIER_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_NAME_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_COUNTRY_MISSING));
    }

    @Test
    void shouldValidateAllFieldsContactIncorrect() {
        final UilDto uilDto = UilDto.builder()
                .eFTIDataUuid("12345678-ab12-4ab6-8999-123456789abc")
                .eFTIPlatformUrl("http://efti.platform.borduria.eu")
                .eFTIGateUrl("http://efti.gate.borduria.eu")
                .authority(AuthorityDto.builder()
                        .name("name")
                        .country("dadada")
                        .legalContact(ContactInformationDto.builder()
                                .streetName("street")
                                .city("city")
                                .email("pas un email")
                                .postalCode("62320")
                                .buildingNumber("12").build())
                        .workingContact(ContactInformationDto.builder().build())
                        .isEmergencyService(true)
                        .nationalUniqueIdentifier("national unique identifier")
                        .build()).build();

        final Validator validator;
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }

        final Set<ConstraintViolation<UilDto>> violations = validator.validate(uilDto);
        assertFalse(violations.isEmpty());
        assertEquals(13, violations.size());
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_COUNTRY_TOO_LONG));
        assertTrue(containsError(violations, ErrorCodesEnum.AUTHORITY_COUNTRY_UNKNOWN));
        assertTrue(containsError(violations, ErrorCodesEnum.CONTACT_BUILDING_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.CONTACT_POSTAL_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.CONTACT_MAIL_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.CONTACT_CITY_MISSING));
        assertTrue(containsError(violations, ErrorCodesEnum.CONTACT_MAIL_INCORRECT_FORMAT));
        assertTrue(containsError(violations, ErrorCodesEnum.CONTACT_STREET_NAME_MISSING));
    }

    private boolean containsError(final Set<ConstraintViolation<UilDto>> violations, final ErrorCodesEnum error) {
        for(final ConstraintViolation<UilDto> violation : violations ) {
            if(violation.getMessage().equals(error.name())) {
                return true;
            }
        }
        return false;
    }
}
