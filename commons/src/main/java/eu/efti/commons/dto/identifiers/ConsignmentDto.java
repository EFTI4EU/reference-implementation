package eu.efti.commons.dto.identifiers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ConsignmentDto implements Serializable {
    @JsonIgnore
    private long id;
    private String platformId;
    private String datasetId;
    private String gateId;
    private OffsetDateTime carrierAcceptanceDatetime;
    private OffsetDateTime deliveryEventActualOccurrenceDatetime;
    private List<MainCarriageTransportMovementDto> mainCarriageTransportMovements;
    private List<UsedTransportEquipmentDto> usedTransportEquipments;
}
