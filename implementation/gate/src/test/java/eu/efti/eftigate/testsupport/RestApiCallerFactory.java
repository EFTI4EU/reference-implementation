package eu.efti.eftigate.testsupport;

import eu.efti.eftigate.config.security.RestApiRoles;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.logging.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.util.function.Consumer;

// Note: lazy initialization to ensure @LocalServerPort is initialized before we read it
@Lazy
@Component
public class RestApiCallerFactory {
    public record RestApiCaller(WebTestClient webTestClient) {
        public <T> EntityExchangeResult<T> get(String url, Class<T> clazz) {
            return webTestClient.get().uri(url).exchange().expectBody(clazz).returnResult();
        }
    }

    @LocalServerPort
    private int port;

    public RestApiCaller createUnauthenticated() {
        return getRestApiCaller(h -> {});
    }

    public RestApiCaller createAuthenticatedForPlatformApi(String platformId) {
        return createAuthenticatedWithRole(platformId, RestApiRoles.ROLE_PLATFORM);
    }

    public RestApiCaller createAuthenticatedWithRole(String platformId, String role) {
        return getRestApiCaller(h -> h
                .set("X-Pre-Authenticated-User-Id", platformId)
                .set("X-Pre-Authenticated-User-Role", role));
    }

    private @NotNull RestApiCaller getRestApiCaller(Consumer<HttpHeaders> headersCustomizer) {
        var httpClient = HttpClient.create()
                .wiretap(
                        RestApiCaller.class.getCanonicalName(),
                        LogLevel.INFO,
                        AdvancedByteBufFormat.TEXTUAL
                )
                .headers(headersCustomizer);

        var testClient = WebTestClient.bindToServer(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:" + port)
                .build();

        return new RestApiCaller(testClient);
    }
}