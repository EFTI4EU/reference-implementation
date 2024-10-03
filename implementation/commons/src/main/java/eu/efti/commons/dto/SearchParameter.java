package eu.efti.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchParameter implements Serializable {
    private String vehicleID;
    private String identifierType;
    private Boolean isDangerousGoods;
    private String vehicleCountry;
    private String transportMode;
}
