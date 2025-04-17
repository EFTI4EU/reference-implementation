package eu.efti.platformgatesimulator;

import eu.efti.platformgatesimulator.service.GateIntegrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Slf4j
public class PlatformGateSimulatorApplication {
    @Component
    @AllArgsConstructor
    public static class StartupRoutines {
        private GateIntegrationService gateIntegrationService;

        @EventListener
        @Async
        public void onApplicationEvent(@SuppressWarnings("unused") ApplicationStartedEvent event) {
            try {
                var whoami = gateIntegrationService.callWhoami();
                log.info("Connected as {} to gate at {}", whoami, gateIntegrationService.getRestApiBaseUrl());
            } catch (GateIntegrationService.GateIntegrationServiceException e) {
                log.warn("Could not connect to gate at {} because of {}", gateIntegrationService.getRestApiBaseUrl(), e.getMessage());
            } catch (Exception e) {
                log.error("Unhandled error in startup routines", e);
            }
        }
    }

    public static void main(final String[] args) {
        SpringApplication.run(PlatformGateSimulatorApplication.class, args);
    }

}
