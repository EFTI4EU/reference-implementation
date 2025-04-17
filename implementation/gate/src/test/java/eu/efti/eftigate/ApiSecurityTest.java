package eu.efti.eftigate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class ApiSecurityTest extends IntegrationTest {
    @Autowired
    protected RestApiCallerFactory restApiCallerFactory;

    @Test
    public void nonPreAuthenticatedPlatformShouldNotHaveAccessToPlatformApi() {
        var caller = restApiCallerFactory.createUnauthenticated();
        var res = caller.get("/api/platform/v0/whoami", String.class);
        assertAll(
                () -> assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus()),
                () -> assertNull(res.getResponseBody())
        );
    }

    @Test
    public void preAuthenticatedPlatformShouldHaveAccessToPlatformApi() {
        var somePlatformId = "some-platform";
        var caller = restApiCallerFactory.createForPlatformApi(somePlatformId);
        var res = caller.get("/api/platform/v0/whoami", String.class);
        assertAll(
                () -> assertEquals(HttpStatus.OK, res.getStatus()),
                () -> assertEquals(
                        "<whoamiResponse><appId>" + somePlatformId + "</appId><role>PLATFORM</role></whoamiResponse>",
                        res.getResponseBody())
        );
    }

    @Test
    public void preAuthenticatedPlatformShouldNotHaveAccessToWSApi() {
        var caller = restApiCallerFactory.createForPlatformApi("some-platform");
        var res = caller.get("/ws", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus());
    }
}
