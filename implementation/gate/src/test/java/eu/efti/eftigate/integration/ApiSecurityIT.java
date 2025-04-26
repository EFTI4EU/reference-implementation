package eu.efti.eftigate.integration;

import eu.efti.eftigate.testsupport.RestIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static eu.efti.testsupport.TestData.randomIdentifier;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ApiSecurityIT extends RestIntegrationTest {
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
        var platformId = randomIdentifier();
        var caller = restApiCallerFactory.createAuthenticatedForPlatformApi(platformId);
        var res = caller.get("/api/platform/v0/whoami", String.class);
        assertAll(
                () -> assertEquals(HttpStatus.OK, res.getStatus()),
                () -> assertEquals(
                        "<whoamiResponse><appId>" + platformId + "</appId><role>PLATFORM</role></whoamiResponse>",
                        res.getResponseBody())
        );
    }

    @Test
    public void preAuthenticatedWithNonPlatformRoleShouldNotHaveAccessToPlatformApi() {
        var caller = restApiCallerFactory.createAuthenticatedWithRole(randomIdentifier(), randomIdentifier());
        var res = caller.get("/api/platform/v0/whoami", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus());
    }

    @Test
    public void preAuthenticatedPlatformShouldNotHaveAccessToApiRoot() {
        var caller = restApiCallerFactory.createAuthenticatedForPlatformApi(randomIdentifier());
        var res = caller.get("/api", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus());
    }

    @Test
    public void preAuthenticatedPlatformShouldNotHaveAccessToWSApi() {
        var caller = restApiCallerFactory.createAuthenticatedForPlatformApi(randomIdentifier());
        var res = caller.get("/ws", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus());
    }
}
