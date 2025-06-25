package eu.efti.eftigate.feign;

import com.github.tomakehurst.wiremock.WireMockServer;
import eu.efti.eftigate.config.feign.TestFeignConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

@SpringBootTest(
        classes = {PlatformClientTest.class, TestFeignConfig.class}
)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlatformClientTest {

    @Autowired
    private PlatformClient platformClient;

    WireMockServer wireMockServer;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(6062);
        wireMockServer.start();
    }

    @Test
    @WithMockUser
    void testSendUilQuery() {
        wireMockServer.stubFor(get("/v0/consignments/123?subsetId=FI01")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<?xml >")));

        ResponseEntity<String> response = platformClient.sendUilQuery(URI.create("http://localhost:6062"), "123", List.of("FI01"));

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("<?xml >", response.getBody());
        Assertions.assertEquals("application/xml", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
    }

    @Test
    @WithMockUser
    void testPostFollowUp() {
        wireMockServer.stubFor(post("/v0/consignments/123/follow-up")
                .willReturn(aResponse()
                        .withStatus(204)
                        .withBody("follow up com")));

        ResponseEntity<Void> response = platformClient.postConsignmentFollowUp(URI.create("http://localhost:6062"), "123", "follow up com");

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @AfterEach
    void teardown() {
        wireMockServer.stop();
    }
}
