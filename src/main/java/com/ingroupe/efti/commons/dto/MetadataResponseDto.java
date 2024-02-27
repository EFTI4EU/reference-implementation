package com.ingroupe.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ingroupe.efti.commons.enums.CountryIndicator;
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
        "requestUuid",
        "status",
        "errorDescription",
        "metadata"
})
public class MetadataResponseDto {
    private CountryIndicator eFTIGate;
    private String requestUuid;
    private String status;
    private String errorCode;
    private String errorDescription;
    private List<MetadataResultDto> metadata;
}
