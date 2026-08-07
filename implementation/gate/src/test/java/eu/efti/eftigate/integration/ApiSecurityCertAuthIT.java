package eu.efti.eftigate.integration;

import eu.efti.eftigate.testsupport.RestIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static eu.efti.testsupport.TestData.randomIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles({"it", "certAuth"})
public class ApiSecurityCertAuthIT extends RestIntegrationTest {
    @Test
    public void nonAuthenticatedUserShouldNotHaveAccessToAapControlApi() {
        var caller = restApiCallerFactory.createUnauthenticated();
        var res = caller.get("/v1/aap/control/uil", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus());
    }

    @Test
    public void preAuthenticatedPlatformHeadersShouldNotHaveAccessToAapControlApi() {
        var caller = restApiCallerFactory.createAuthenticatedForPlatformApi(randomIdentifier());
        var res = caller.get("/v1/aap/control/uil", Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatus());
    }

    @Test
    public void preAuthenticatedPlatformShouldHaveAccessToPlatformApi() {
        var caller = restApiCallerFactory.createAuthenticatedForPlatformApi(randomIdentifier());
        var res = caller.get("/api/platform/v0/whoami", String.class);
        assertEquals(HttpStatus.OK, res.getStatus());
    }
}


