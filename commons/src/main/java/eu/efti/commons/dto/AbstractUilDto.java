package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractUilDto {

    private static final String REGEX_URI = "^[-@./#&+\\w\\s]*$";

    @NotNull(message = "UIL_GATE_MISSING")
    @NotBlank(message = "UIL_GATE_MISSING")
    @Size(max = 255, message = "GATE_ID_TOO_LONG")
    @Pattern(regexp = REGEX_URI, message = "GATE_ID_INCORRECT_FORMAT")
    @Schema(example = "regex = ^[-@./#&+\\w\\s]*$")
    private String gateId;

    @NotNull(message = "UIL_UUID_MISSING")
    @NotBlank(message = "UIL_UUID_MISSING")
    @Size(max = 36, message = "DATASET_ID_TOO_LONG")
    @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", message = "DATASET_ID_INCORRECT_FORMAT")
    @Schema(example = "regex = [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
    @JsonProperty("datasetId")
    private String datasetId;

    @NotNull(message = "UIL_PLATFORM_MISSING")
    @NotBlank(message = "UIL_PLATFORM_MISSING")
    @Size(max = 255, message = "PLATFORM_ID_TOO_LONG")
    @Pattern(regexp = REGEX_URI, message = "PLATFORM_ID_INCORRECT_FORMAT")
    @Schema(example = "regex = ^[-@./#&+\\w\\s]*$")
    private String platformId;
}
