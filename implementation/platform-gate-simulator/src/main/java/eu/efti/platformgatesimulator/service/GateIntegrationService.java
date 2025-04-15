package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.service.client.DefaultApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;

@Service
public class GateIntegrationService {
    public static class GateIntegrationServiceException extends Exception {
        public GateIntegrationServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final DefaultApi api;

    private final GateProperties gateProperties;

    @Autowired
    public GateIntegrationService(GateProperties gateProperties) {
        this.gateProperties = gateProperties;
        var builder = new RestTemplateBuilder()
                .defaultHeader("X-Mock-Pre-Authenticated-User-Id", gateProperties.getOwner())
                .defaultHeader("X-Mock-Pre-Authenticated-User-Role", "PLATFORM");
        api = new DefaultApi(new ApiClient(builder.build())
                .setBasePath(gateProperties.getRestApiBaseUrl().toString()));
    }

    public URI getRestApiBaseUrl() {
        return gateProperties.getRestApiBaseUrl();
    }

    public String callWhoami() throws GateIntegrationServiceException {
        try {
            return api.getWhoami().getAppId();
        } catch (HttpClientErrorException e) {
            throw new GateIntegrationServiceException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
