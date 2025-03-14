package eu.efti.commons.dto.identifiers.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "schemeAgencyId",
        "sequenceNumber"
})
public class CarriedTransportEquipmentApiDto implements Serializable {
    private String id;
    private int sequenceNumber;
    private String schemeAgencyId;
}
