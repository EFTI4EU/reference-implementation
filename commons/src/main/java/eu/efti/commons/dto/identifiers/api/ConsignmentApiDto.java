package eu.efti.commons.dto.identifiers.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsignmentApiDto implements Serializable {
    private String platformId;
    private String datasetId;
    private String gateId;
    private OffsetDateTime carrierAcceptanceDatetime;
    private OffsetDateTime deliveryEventActualOccurrenceDatetime;
    @JsonProperty("mainCarriageTransportMovement")
    private List<MainCarriageTransportMovementApiDto> mainCarriageTransportMovements;
    @JsonProperty("usedTransportEquipment")
    private List<UsedTransportEquipmentApiDto> usedTransportEquipments;
}
