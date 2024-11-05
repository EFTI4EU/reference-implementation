package eu.efti.eftilogger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class LogRequestDto extends LogCommonDto {

    public final String requestId;
    @JsonProperty("eFTIDataId")
    public final String eftidataId;
    public final String responseId;
    public final String authorityNationalUniqueIdentifier;
    public final String authorityName;
    public final String officerId;
    public final String subsetId;
    public final String subsetMSRequested;
    public final String requestType;
}
