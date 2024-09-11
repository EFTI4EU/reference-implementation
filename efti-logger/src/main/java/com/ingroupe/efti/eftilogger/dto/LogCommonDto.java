package com.ingroupe.efti.eftilogger.dto;

import com.ingroupe.efti.eftilogger.model.ComponentType;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
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
    private String timeoutComponentType;
}
