package com.ingroupe.platform.platformgatesimulator;

import com.ingroupe.platform.platformgatesimulator.config.GateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;


@SpringBootApplication
public class PlatformGateSimulatorApplication {

    public static void main(final String[] args) {
        SpringApplication.run(PlatformGateSimulatorApplication.class, args);
    }

}
