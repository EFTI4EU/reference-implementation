package eu.efti.eftilogger.dto;

import eu.efti.eftilogger.model.ComponentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessagePartiesDto {

    private ComponentType requestingComponentType;
    private String requestingComponentId;
    private String requestingComponentCountry;
    private ComponentType respondingComponentType;
    private String respondingComponentId;
    private String respondingComponentCountry;
    private String requestType;
}
