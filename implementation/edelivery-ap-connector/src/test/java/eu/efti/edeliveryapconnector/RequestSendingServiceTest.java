package eu.efti.edeliveryapconnector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
class RequestSendingServiceTest {

    private RequestSendingService service;
    private static final String FOLDER = "src/test/resources/wiremock";
    private WireMockServer wireMockServer;
    private final String requestId = UUID.randomUUID().toString();


    @BeforeEach
    void init() {
        service = new RequestSendingService();

        wireMockServer = new WireMockServer(WireMockConfiguration
                .wireMockConfig().withRootDirectory(FOLDER).dynamicPort()
                .notifier(new ConsoleNotifier(true)));
        wireMockServer.start();
    }

    @Test
    void shouldBuildRequest() throws SendRequestException {
        wireMockServer.stubFor(get(urlEqualTo("/domibus/services/wsplugin?wsdl"))
                .willReturn(aResponse().withBodyFile("WebServicePlugin.wsdl")));
        wireMockServer.stubFor(post(urlEqualTo("/domibus/services/wsplugin?wsdl"))
                .willReturn(aResponse().withBodyFile("response.xml")));

        final ApRequestDto requestDto = ApRequestDto
            .builder()
            .sender("syldavia")
            .receiver("borduria")
            .body("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=")
            .apConfig(ApConfigDto.
                    builder()
                    .url(String.format("http://localhost:%s/domibus/services/wsplugin?wsdl", wireMockServer.port()))
                    .username("username")
                    .password("password")
                    .build()).build();

        final String result = service.sendRequest(requestDto);
        assertEquals("fc0e70cf-8d57-11ee-a62e-0242ac13000d@domibus.eu", result);
    }

    @Test
    void shouldThrowExceptionIfResponseEmpty() throws SendRequestException {
        wireMockServer.stubFor(get(urlEqualTo("/domibus/services/wsplugin?wsdl"))
                .willReturn(aResponse().withBodyFile("WebServicePlugin.wsdl")));
        wireMockServer.stubFor(post(urlEqualTo("/domibus/services/wsplugin?wsdl"))
                .willReturn(aResponse().withBodyFile("emptyresponse.xml")));
        final ApRequestDto requestDto = ApRequestDto
            .builder()
            .sender("syldavia")
            .receiver("borduria")
            .requestId(requestId)
                .body("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=")
            .apConfig(ApConfigDto.
                    builder()
                    .url(String.format("http://localhost:%s/domibus/services/wsplugin?wsdl", wireMockServer.port()))
                    .username("username")
                    .password("password")
                    .build()).build();

        final SendRequestException exception = assertThrows(SendRequestException.class, () -> service.sendRequest(requestDto));
        assertEquals(String.format("no messageId for request %s", requestId), exception.getMessage());

    }
}
