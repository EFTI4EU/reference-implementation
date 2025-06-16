package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.connector.KeycloakConnector;
import eu.efti.platformgatesimulator.controller.feign.CallGateFeign;
import eu.efti.platformgatesimulator.dto.KeycloakDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallGateService {

    private final CallGateFeign callGateFeign;
    private final KeycloakConnector keycloakConnector;

    public ResponseEntity<String> sendGate(final String result, final String datasetId) {
        Map<String, String> headers = new HashMap<>();
        KeycloakDto keycloakDto = keycloakConnector.getServiceAccountToken();
        headers.put("Authorization", "Bearer " + keycloakDto.getAccessToken());
        headers.put("Referer", "");
        headers.put("Content-Type", "application/xml");
        headers.put("Accept", "application/xml");

        return callGateFeign.sendIdentifiers(headers, datasetId, result);
    }
}
