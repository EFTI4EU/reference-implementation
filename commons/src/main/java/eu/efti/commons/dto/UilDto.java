package eu.efti.commons.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UilDto implements ValidableDto {
    private static final String REGEX_URI = "^[-@./#&+\\w\\s]*$";

    @NotBlank(message = "GATE_ID_MISSING")
    @Size(max = 255, message = "GATE_ID_TOO_LONG")
    @Pattern(regexp = REGEX_URI, message = "GATE_ID_INCORRECT_FORMAT")
    @Schema(example = "regex = ^[-@./#&+\\w\\s]*$")
    private String gateId;

    @NotBlank(message = "DATASET_ID_MISSING")
    @Size(max = 36, message = "DATASET_ID_TOO_LONG")
    @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", message = "DATASET_ID_INCORRECT_FORMAT")
    @Schema(example = "regex = [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
    private String datasetId;

    @NotBlank(message = "PLATFORM_ID_MISSING")
    @Size(max = 255, message = "PLATFORM_ID_TOO_LONG")
    @Pattern(regexp = REGEX_URI, message = "PLATFORM_ID_INCORRECT_FORMAT")
    @Schema(example = "regex = ^[-@./#&+\\w\\s]*$")
    private String platformId;

    private String subsetId;
}
