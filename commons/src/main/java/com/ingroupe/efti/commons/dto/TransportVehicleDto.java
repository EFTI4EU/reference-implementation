package com.ingroupe.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransportVehicleDto implements Serializable {
    @JsonIgnore
    private long id;
    private String transportMode;
    @Max(value = 999, message = "SEQUENCE_TOO_LONG")
    private int sequence;
    @NotNull(message = "VEHICLE_ID_MISSING")
    @Length(max = 17, message = "VEHICLE_ID_TOO_LONG")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "VEHICLE_ID_INCORRECT_FORMAT")
    @JsonProperty("vehicleID")
    private String vehicleID;
    private String vehicleCountry;
    private String journeyStart;
    private String countryStart;
    private String journeyEnd;
    private String countryEnd;
}
