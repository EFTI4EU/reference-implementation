package com.ingroupe.efti.eftigate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUuidDto {

    @NotNull
    private String requestUuid;

    @NotNull
    private String status;
    private String errorCode;
    private String errorDescription;
    @JsonProperty("eFTIData")
    private byte[] eFTIData;
}