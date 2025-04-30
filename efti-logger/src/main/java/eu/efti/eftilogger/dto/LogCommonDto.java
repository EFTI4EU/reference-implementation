package eu.efti.eftilogger.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.efti.eftilogger.model.ComponentType;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogCommonDto {

    private String messageDate;
    private String name;
    private ComponentType componentType;
    private String componentId;
    private String componentCountry;
    private ComponentType requestingComponentType;
    private String requestingComponentId;
    private String requestingComponentCountry;
    private ComponentType respondingComponentType;
    private String respondingComponentId;
    private String respondingComponentCountry;
    private String messageContent;
    private String statusMessage;
    private String errorCodeMessage;
    private String errorDescriptionMessage;
    private String sentDate;
    private Long responseDelay;
    @JsonProperty("eFTIDataId")
    private String eFTIDataId;
}
