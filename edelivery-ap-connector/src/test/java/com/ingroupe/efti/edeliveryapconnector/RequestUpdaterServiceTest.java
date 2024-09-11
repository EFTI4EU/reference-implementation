package com.ingroupe.efti.edeliveryapconnector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ingroupe.efti.edeliveryapconnector.dto.ApConfigDto;
import com.ingroupe.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.domibus.plugin.ws.generated.MarkMessageAsDownloadedFault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.MalformedURLException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@ExtendWith(SpringExtension.class)
class RequestUpdaterServiceTest {

    private RequestUpdaterService service;
    private final static String FOLDER = "src/test/java/resources/wiremock";
    private WireMockServer wireMockServer;

    @BeforeEach
    void init() {
        service = new RequestUpdaterService();

        wireMockServer = new WireMockServer(WireMockConfiguration
                .wireMockConfig().withRootDirectory(FOLDER).dynamicPort()
                .notifier(new ConsoleNotifier(true)));
        wireMockServer.start();
    }

    @Test
    void setMarkedAsDownloadTest() throws MalformedURLException, MarkMessageAsDownloadedFault {
        wireMockServer.stubFor(get(urlEqualTo("/domibus/services/wsplugin?wsdl"))
                .willReturn(aResponse().withBodyFile("WebServicePlugin.wsdl")));
        wireMockServer.stubFor(post(urlEqualTo("/domibus/services/wsplugin?wsdl"))
                .willReturn(aResponse().withBodyFile("retrieve-response.xml")));

        final ApConfigDto apConfigDto = ApConfigDto.builder()
                .url(String.format("http://localhost:%s/domibus/services/wsplugin?wsdl", wireMockServer.port()))
                .username("username")
                .password("password")
                .build();

        service.setMarkedAsDownload(apConfigDto, "messageId");

        wireMockServer.verify(new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO,1), postRequestedFor(urlEqualTo("/domibus/services/wsplugin?wsdl")));
    }
}
