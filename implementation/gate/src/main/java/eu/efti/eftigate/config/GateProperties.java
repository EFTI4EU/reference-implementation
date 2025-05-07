package eu.efti.eftigate.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GateProperties {
    private String country;
    private String owner;
    private ApConfig ap;
    private List<PlatformProperties> platforms;

    @Data
    @Builder
    public static final class ApConfig {
        private String url;
        private String username;
        private String password;
    }

    @Builder
    public record PlatformProperties(String platformId, Boolean useRestApi, URI restApiBaseUrl) {
        public PlatformProperties {
            if (useRestApi == null) {
                useRestApi = false;
            }
            if (useRestApi) {
                if (restApiBaseUrl == null) {
                    throw new IllegalArgumentException("restApiBaseUrl must not be null");
                }
            } else if (restApiBaseUrl != null) {
                throw new IllegalArgumentException("restApiBaseUrl must be null");
            }
        }
    }

    public boolean isCurrentGate(final String gateId) {
        return this.owner.equalsIgnoreCase(gateId);
    }
}
