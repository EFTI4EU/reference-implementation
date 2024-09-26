package eu.efti.eftigate.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GateProperties {
    private String country;
    private String owner;
    private ApConfig ap;

    @Data
    @Builder
    public static final class ApConfig {
        private String url;
        private String username;
        private String password;
    }

    public boolean isCurrentGate(final String gateUrl) {
        return this.owner.equals(gateUrl);
    }
}
