package com.ingroupe.efti.commons.dto;

import com.ingroupe.efti.commons.enums.CountryIndicator;
import com.ingroupe.efti.commons.validator.ValueOfEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityDto {

    private int id;
    @NotBlank(message = "AUTHORITY_COUNTRY_MISSING")
    @Length(max = 2, message = "AUTHORITY_COUNTRY_TOO_LONG")
    @ValueOfEnum(enumClass = CountryIndicator.class, message = "AUTHORITY_COUNTRY_UNKNOWN")
    private String country;
    @Valid
    @NotNull(message = "AUTHORITY_LEGAL_CONTACT_MISSING")
    private ContactInformationDto legalContact;
    @Valid
    @NotNull(message = "AUTHORITY_WORKING_CONTACT_MISSING")
    private ContactInformationDto workingContact;
    @NotNull(message = "AUTHORITY_IS_EMERGENCY_MISSING")
    private Boolean isEmergencyService;
    @NotBlank(message = "AUTHORITY_NAME_MISSING")
    @Length(max = 100, message = "AUTHORITY_NAME_TOO_LONG")
    private String name;
    @NotBlank(message = "AUTHORITY_NATIONAL_IDENTIFIER_MISSING")
    @Length(max = 100, message = "AUTHORITY_NATIONAL_IDENTIFIER_TOO_LONG")
    private String nationalUniqueIdentifier;
}
