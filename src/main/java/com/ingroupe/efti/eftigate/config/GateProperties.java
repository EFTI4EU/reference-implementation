package com.ingroupe.efti.eftigate.config;

import com.ingroupe.efti.eftigate.dto.RequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GateProperties {
    private String owner;
    private ApConfig ap;

    @Data
    @Builder
    public static final class ApConfig {
        private String url;
        private String username;
        private String password;
    }

    public boolean isCurrentGate(String gateUrl) {
        return this.owner.equals(gateUrl);
    }
}
