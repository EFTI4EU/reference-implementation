package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@XmlRootElement(name = "body")
public class IdentifiersDto extends AbstractUilDto {

    private long id;
    @JsonProperty("isDangerousGoods")
    @XmlElement(name = "isDangerousGoods")
    private Boolean isDangerousGoods;
    private String journeyStart;
    private String countryStart;
    private String journeyEnd;
    private String countryEnd;
    private String identifiersUUID;
    @NotEmpty(message = "TRANSPORT_VEHICLES_MISSING")
    @Valid
    private List<TransportVehicleDto> transportVehicles = new ArrayList<>();
    @JsonProperty("isDisabled")
    @XmlElement(name = "isDisabled")
    private boolean isDisabled;
}
