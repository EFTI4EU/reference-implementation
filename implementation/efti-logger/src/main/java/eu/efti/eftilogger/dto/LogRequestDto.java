package eu.efti.eftilogger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
public class LogRequestDto extends LogCommonDto {

    public final String requestId;
    @JsonProperty("eFTIDataId")
    public final String eftidataId;
    public final String authorityNationalUniqueIdentifier;
    public final String authorityName;
    public final String subsetId;
    public final String requestType;
}
