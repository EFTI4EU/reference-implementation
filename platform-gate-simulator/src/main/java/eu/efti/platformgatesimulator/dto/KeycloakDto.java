package eu.efti.platformgatesimulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakDto {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private long expires;
}
