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
    public final String authorityNationalUniqueIdentifier;
    public final String authorityName;
    public final String subsetId;
    public final String requestType;
}
