package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataResultDto implements Serializable {
    @JsonIgnore
    private long id;
    @JsonProperty("eFTIGateUrl")
    private String eFTIGateUrl;
    @JsonProperty("eFTIDataUuid")
    private String eFTIDataUuid;
    @JsonProperty("eFTIPlatformUrl")
    private String eFTIPlatformUrl;
    @JsonProperty("isDangerousGoods")
    private Boolean isDangerousGoods;
    private String journeyStart;
    private String countryStart;
    private String journeyEnd;
    private String countryEnd;
    private String metadataUUID;
    @NotEmpty(message = "TRANSPORT_VEHICLES_MISSING")
    @Valid
    private List<TransportVehicleDto> transportVehicles;
    @JsonIgnore
    private boolean isDisabled;
}
