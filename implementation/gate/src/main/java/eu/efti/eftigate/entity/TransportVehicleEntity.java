package eu.efti.eftigate.entity;
import eu.efti.commons.enums.CountryIndicator;
import eu.efti.commons.enums.TransportMode;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransportVehicleEntity extends JourneyEntity implements Serializable {

    private int id;
    @Enumerated(EnumType.STRING)
    private TransportMode transportMode;
    private int sequence;
    private String vehicleID;
    @Enumerated(EnumType.STRING)
    private CountryIndicator vehicleCountry;
}
