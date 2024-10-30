package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.StatusEnum;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        "eFTIGate",
        "requestId",
        "status",
        "errorCode",
        "errorDescription",
        "identifiers"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "body")
public class IdentifiersResponseDto {
    @JsonProperty("eFTIGate")
    @XmlElement(name = "eFTIGate")
    private CountryIndicator eFTIGate;
    private String requestId;
    private StatusEnum status;
    private String errorCode;
    private String errorDescription;
    private List<ConsignmentDto> identifiers;
}
