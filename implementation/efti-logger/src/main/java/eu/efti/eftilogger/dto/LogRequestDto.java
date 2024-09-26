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
    public final String responseId;
    public final String authorityNationalUniqueIdentifier;
    public final String authorityName;
    public final String officerId;
    public final String subsetEURequested;
    public final String subsetMSRequested;
    public final String requestType;
}
