package eu.efti.commons.dto.identifiers;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class UsedTransportEquipmentDto implements Serializable {
    private long id;
    private int sequenceNumber;
    private String equipmentId;
    private String idSchemeAgencyId;
    private String registrationCountry;
    private List<CarriedTransportEquipmentDto> carriedTransportEquipments;
}
