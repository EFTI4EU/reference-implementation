package eu.efti.commons.dto.identifiers;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class CarriedTransportEquipmentDto implements Serializable {
    private long id;
    private int sequenceNumber;
    private String equipmentId;
    private String schemeAgencyId;
}
