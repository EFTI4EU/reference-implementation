package eu.efti.platformgatesimulator.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GateProperties {
    private String owner;
    private String cdaPath;
    private String gate;
    private ApConfig ap;
    private URI restApiBaseUrl;
    private int minSleep;
    private int maxSleep;

    @Data
    @Builder
    public static final class ApConfig {
        private String url;
        private String username;
        private String password;
    }
}
