package com.ingroupe.platform.platformgatesimulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BodyDto {
    @JsonProperty
    private String eFTIData;
    private String status;
    private String requestUuid;
    @JsonProperty("eFTIDataUuid")
    private String eFTIDataUuid;
    private String errorDescription;
}
