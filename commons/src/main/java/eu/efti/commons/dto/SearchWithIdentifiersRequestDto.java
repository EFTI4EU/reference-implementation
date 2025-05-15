package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.validator.ValueOfEnum;
import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.edelivery.IdentifierType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchWithIdentifiersRequestDto implements ValidableDto {

    @Pattern(regexp = "^\\d$", message = "MODE_CODE_INCORRECT_FORMAT")
    private String modeCode;
    @NotBlank(message = "IDENTIFIER_MISSING")
    @Length(max = 17, message = "IDENTIFIER_TOO_LONG")
    @Pattern(regexp = "^[\\w\\u00C0-\\u024F\\u1E00-\\u1EFF\\-— ]*$", message = "IDENTIFIER_INCORRECT_FORMAT")
    private String identifier;
    private List<@Valid @ValueOfEnum(enumClass = IdentifierType.class, message = "IDENTIFIER_TYPE_INCORRECT") String> identifierType;
    @ValueOfEnum(enumClass = CountryCode.class, message = "REGISTRATION_COUNTRY_INCORRECT")
    private String registrationCountryCode;
    @JsonProperty("dangerousGoodsIndicator")
    private Boolean dangerousGoodsIndicator;
    @JsonProperty("eftiGateIndicator")
    private List<@Valid @ValueOfEnum(enumClass = CountryIndicator.class, message = "GATE_INDICATOR_INCORRECT") String> eftiGateIndicator;
    private AuthorityDto authority;
}
