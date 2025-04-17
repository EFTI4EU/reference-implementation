package eu.efti.eftigate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.efti.eftigate.config.security.converters.KeycloakResourceRolesConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.Jwt.Builder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakResourceRolesConverterTest {

    @Spy
    private final KeycloakResourceRolesConverter converter = new KeycloakResourceRolesConverter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(converter, "issuers", List.of("http://auth.gate.borduria.eu:8080/realms/eFTI_BO", "https://identite-sandbox.proconnect.gouv.fr"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void convert() {
        final String keycloakToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIwT0FDN0MybDBZMXhRd1VWZTlFYVpZSWtQR1NDaVRLV25BWXJPcVd0cWx3In0.eyJleHAiOjE3NDQ4MDcwNjcsImlhdCI6MTc0NDgwNjQ2NywianRpIjoiNGMwM2YxOTQtZjMyNi00MTQ2LTg2MTctNzhkZDI1OTAxNDE2IiwiaXNzIjoiaHR0cDovL2F1dGguZ2F0ZS5ib3JkdXJpYS5ldTo4MDgwL3JlYWxtcy9lRlRJX0JPIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjNiZWJkMDMzLTZiZGItNGNjNy1hMDc0LWNlOWY4MjMxNzU1MiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImdhdGUiLCJzZXNzaW9uX3N0YXRlIjoiNzA4ZTgzZmMtN2I5NS00ZjQyLTk0OTUtNjMzYzhiODRmYzU5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiIsIlJPQURfQ09OVFJPTEVSIiwiZGVmYXVsdC1yb2xlcy1lZnRpX2JvIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNzA4ZTgzZmMtN2I5NS00ZjQyLTk0OTUtNjMzYzhiODRmYzU5IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXJfYm8ifQ.YFc8Xnm4o36eogCrq_U2PFucOqFE04i5DXI14P6Wvi5f__KD8CE5Q-qB0ufe9re7q3g0jOyU7FyEP908T85jimxk3-vb6BKFb5JupXmdpjAh_YVZMbp54G_WqCMLEuZL5mlFyz6y2CA3sU9SgICcJZaqdH69O-5Rhan6xntf-JHR96PmeBs582SeeH-j6wokdLf7uiS4_KaFcwCdTOx2a1TTTHh0tKxdfxz2mm8NnsWdsmDhhuD2Fw6QLqqzJgIukx575G0mcIRBXcPh-sP0Yoa1rVjRhJnHM8YnvQkggHV8-w8B7o9gUa3bFwDfb-wff7U7KIPgeJqLQp_E8bGk7w";
        final DecodedJWT decoded = JWT.decode(keycloakToken);
        final Builder jwtBuilder = Jwt.withTokenValue(keycloakToken);

        List.of("alg", "typ", "kid").forEach(key -> jwtBuilder.header(key, decoded.getHeaderClaim(key).asString()));

        final Jwt jwtToken = jwtBuilder.claim("azp", decoded.getClaim("azp").asString())
                .claim("realm_access", decoded.getClaim("realm_access").asMap())
                .claim("resource_access", decoded.getClaim("resource_access").asMap())
                .claim("iss", decoded.getClaim("iss").asString())
                .build();

        final Collection<GrantedAuthority> authorities = converter.convert(jwtToken);

        final List<String> realmRoles = (List<String>) decoded.getClaim("realm_access").asMap().get("roles");
        final List<String> resourceAccessRoles = getResourceAccessRoles(decoded, jwtToken);
        assertTrue(CollectionUtils.isNotEmpty(authorities));
        assertEquals(realmRoles.size() + resourceAccessRoles.size(), authorities.size());
        final List<String> authoritiesAsStringList = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        realmRoles.forEach(role -> assertTrue(authoritiesAsStringList.contains(Roles.ROLE_PREFIX + role)));
        resourceAccessRoles.forEach(role -> assertTrue(authoritiesAsStringList.contains(Roles.ROLE_PREFIX + role)));
    }

    private static List<String> getResourceAccessRoles(DecodedJWT decoded, Jwt jwtToken) {
        String azpClaim = jwtToken.getClaim("azp");
        Claim resourceAccessClaim = decoded.getClaim("resource_access");
        if (resourceAccessClaim != null && StringUtils.isNotBlank(azpClaim) && resourceAccessClaim.asMap().get(azpClaim) != null) {
            return (List<String>) ((Map<String, Object>) resourceAccessClaim.asMap().get(azpClaim)).get("roles");
        }
        return new ArrayList<>();
    }
}
