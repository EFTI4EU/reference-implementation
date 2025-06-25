package eu.efti.platformgatesimulator.connector;

import eu.efti.platformgatesimulator.dto.KeycloakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class KeycloakConnectorTest {

    private KeycloakConnector keycloakConnector;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        keycloakConnector = new KeycloakConnector();

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(keycloakConnector, "restTemplate");

        assertNotNull(restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);

        ReflectionTestUtils.setField(keycloakConnector, "clientId", "test-client");
        ReflectionTestUtils.setField(keycloakConnector, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(keycloakConnector, "keycloakUrl", "http://localhost/token");
    }

    @Test
    void testGetServiceAccountToken_success() {
        //Arrange
        String mockResponse = """
                    {
                      "access_token": "mocked-access-token",
                      "expires_in": 3600,
                      "token_type": "Bearer"
                    }
                """;

        mockServer.expect(once(), requestTo("http://localhost/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andExpect(content().string(containsString("client_id=test-client")))
                .andExpect(content().string(containsString("client_secret=test-secret")))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        // Act
        KeycloakDto dto = keycloakConnector.getServiceAccountToken();

        // Assert
        assertNotNull(dto);
        assertEquals("mocked-access-token", dto.getAccessToken());
        mockServer.verify();
    }
}
