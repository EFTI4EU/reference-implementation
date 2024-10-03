package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.TransportMode;
import eu.efti.commons.validator.ValueOfEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @ValueOfEnum(enumClass = TransportMode.class, message = "TRANSPORT_MODE_INCORRECT")
    private String transportMode;
    @NotBlank(message = "VEHICLE_ID_MISSING")
    @Length(max = 17, message = "VEHICLE_ID_TOO_LONG")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "VEHICLE_ID_INCORRECT_FORMAT")
    private String vehicleID;
    private List<String> identifierType;
    @ValueOfEnum(enumClass = CountryIndicator.class, message = "VEHICLE_COUNTRY_INCORRECT")
    private String vehicleCountry;
    @JsonProperty("isDangerousGoods")
    private Boolean isDangerousGoods;
    @JsonProperty("eFTIGateIndicator")
    private List<@Valid @ValueOfEnum(enumClass = CountryIndicator.class, message = "GATE_INDICATOR_INCORRECT") String> eFTIGateIndicator;
    @Valid
    @NotNull(message = "AUTHORITY_MISSING")
    @Schema(example = "see AuthorityDto")
    private AuthorityDto authority;
}
