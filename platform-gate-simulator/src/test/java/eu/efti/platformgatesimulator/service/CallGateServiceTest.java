package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.connector.KeycloakConnector;
import eu.efti.platformgatesimulator.controller.feign.CallGateFeign;
import eu.efti.platformgatesimulator.dto.KeycloakDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(GateProperties.class)
class CallGateServiceTest {

    AutoCloseable openMocks;

    @Mock
    private CallGateFeign callGateFeign;

    @Mock
    private KeycloakConnector keycloakConnector;

    private CallGateService callGateService;

    @BeforeEach
    void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        callGateService = new CallGateService(callGateFeign, keycloakConnector);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void sendGateTest() {
        Map<String, String> headers = new HashMap<>();
        KeycloakDto keycloakDto = new KeycloakDto();

        keycloakDto.setAccessToken("any");
        headers.put("Authorization", "Bearer any");
        headers.put("Referer", "");
        headers.put("Content-Type", "application/xml");
        headers.put("Accept", "application/xml");

        Mockito.when(keycloakConnector.getServiceAccountToken()).thenReturn(keycloakDto);
        Mockito.when(callGateFeign.sendIdentifiers(any(), anyString(), anyString())).thenReturn(new ResponseEntity<>("success", HttpStatus.OK));

        ResponseEntity<String> result = callGateService.sendGate("result", "datasetId");

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
