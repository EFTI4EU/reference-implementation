package eu.efti.platformgatesimulator.connector;

import eu.efti.platformgatesimulator.dto.KeycloakDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class KeycloakConnector {

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_CREDENTIALS = "client_credentials";

    private final RestTemplate restTemplate;

    @Value("${feign_gate.clientid}")
    String clientId;

    @Value("${feign_gate.clientsecret}")
    String clientSecret;

    @Value("${feign_gate.keycloak}")
    String keycloakUrl;

    public KeycloakConnector() {
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders createHttpHeaderForURLFormEncoded() {
        HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    public KeycloakDto getServiceAccountToken() {
        log.info("Get token for service account {}...", clientId);
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add(GRANT_TYPE, CLIENT_CREDENTIALS);
        body.add(CLIENT_ID, clientId);
        body.add(CLIENT_SECRET, clientSecret);

        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, this.createHttpHeaderForURLFormEncoded());
        return this.restTemplate.exchange(keycloakUrl, HttpMethod.POST, request, KeycloakDto.class).getBody();
    }
}
