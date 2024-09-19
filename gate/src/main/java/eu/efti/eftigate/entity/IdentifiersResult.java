package eu.efti.eftigate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import eu.efti.identifiersregistry.utils.OffsetDateTimeDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdentifiersResult implements Serializable {
    private long id;
    @JsonProperty("eFTIGateUrl")
    private String eFTIGateUrl;
    @JsonProperty("eFTIDataUuid")
    private String eFTIDataUuid;
    @JsonProperty("eFTIPlatformUrl")
    private String eFTIPlatformUrl;
    @JsonProperty("isDangerousGoods")
    private Boolean isDangerousGoods;
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime journeyStart;
    private String countryStart;
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime journeyEnd;
    private String countryEnd;
    private String identifiersUUID;
    @NotEmpty(message = "TRANSPORT_VEHICLES_MISSING")
    @Valid
    private List<TransportVehicleEntity> transportVehicles;
    @JsonIgnore
    private boolean isDisabled;
}
