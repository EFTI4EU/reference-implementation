package eu.efti.eftigate.dto.requestbody;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.validator.ValueOfEnum;
import jakarta.validation.Valid;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "body")
public class IdentifiersRequestBodyDto {
    private String requestUuid;
    private String transportMode;
    @JsonProperty("vehicleId")
    private String vehicleID;
    private String vehicleCountry;
    private Boolean isDangerousGoods;
    @JsonProperty("eFTIGateIndicator")
    private List<@Valid @ValueOfEnum(enumClass = CountryIndicator.class, message = "GATE_INDICATOR_INCORRECT") String> eFTIGateIndicator;

    public static IdentifiersRequestBodyDto fromControl(final ControlDto controlDto){
        final SearchParameter searchParameter = controlDto.getTransportIdentifiers();
        final IdentifiersRequestBodyDto identifiersRequestBodyDto = new IdentifiersRequestBodyDto();
        identifiersRequestBodyDto.setRequestUuid(controlDto.getRequestUuid());
        if (searchParameter != null){
            identifiersRequestBodyDto.setVehicleID(searchParameter.getVehicleId());
            identifiersRequestBodyDto.setTransportMode(searchParameter.getTransportMode());
            identifiersRequestBodyDto.setIsDangerousGoods(searchParameter.getIsDangerousGoods());
            identifiersRequestBodyDto.setVehicleCountry(searchParameter.getVehicleCountry());
        }
        return identifiersRequestBodyDto;
    }
}
