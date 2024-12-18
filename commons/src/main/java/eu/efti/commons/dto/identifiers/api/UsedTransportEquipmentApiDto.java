package eu.efti.commons.dto.identifiers.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "sequenceNumber",
        "schemeAgencyId",
        "registrationCountryCode",
        "categoryCode",
        "carriedTransportEquipment"
})
public class UsedTransportEquipmentApiDto implements Serializable {
    private String id;
    private int sequenceNumber;
    private String schemeAgencyId;
    private String registrationCountryCode;
    private String categoryCode;
    @JsonProperty("carriedTransportEquipment")
    private List<CarriedTransportEquipmentApiDto> carriedTransportEquipments;
}
