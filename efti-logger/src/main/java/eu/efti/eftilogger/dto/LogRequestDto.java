package eu.efti.eftilogger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class LogRequestDto extends LogCommonDto {

    private String requestId;
    @JsonProperty("eFTIDataId")
    private String eftidataId;
    private List<String> subsetIds;
    private String requestType;
}
