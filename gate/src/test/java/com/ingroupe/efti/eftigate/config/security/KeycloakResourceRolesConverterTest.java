package com.ingroupe.efti.eftigate.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.Jwt.Builder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakResourceRolesConverterTest {

    private final KeycloakResourceRolesConverter converter = new KeycloakResourceRolesConverter();

    @Test
    @SuppressWarnings("unchecked")
    void convert() {
        final String keycloakToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJQS3Q4NU5YVzhjMnF2bEFiODBaN2otY1pZQWFlRWljX0gtY1lJN2hmVkwwIn0.eyJleHAiOjE2NjY3MjAwNjQsImlhdCI6MTY2NjcxOTc2NCwianRpIjoiZjUxMjY5MWQtODY4My00NmNjLWFhMTEtYWIzNDE1MGZlNmFjIiwiaXNzIjoiaHR0cDovL2hvc3QuZG9ja2VyLmludGVybmFsOjgwODUvcmVhbG1zL1NDRk4iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNzg2YWI4NmUtOTI2ZS00ZWI5LTgzYjEtZThmYWEyNWFkNzY2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2ZuLXBvcnRhbCIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiTVlfQkVBVVRJRlVMX1JPTEUiLCJkZWZhdWx0LXJvbGVzLWNmbiIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJjZm4tcG9ydGFsIjp7InJvbGVzIjpbIkNGTl9VU0VSIiwidW1hX3Byb3RlY3Rpb24iXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImNsaWVudElkIjoiY2ZuLXBvcnRhbCIsImNsaWVudEhvc3QiOiIxNzIuMTguMC4xIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtY2ZuLXBvcnRhbCIsImNsaWVudEFkZHJlc3MiOiIxNzIuMTguMC4xIn0.Q8MuI3dvzkwU8plp9VheJSoTKxdTFh9_CdmYepaLlxUmZhxQr2jTfXFOfIQlTojjLdI25nLRm0ReV40NPjnG6eg5k4BtE0GXjvcAU0RkkfW5mCpo6VgCSCT3lMBghDKxerdOaTatykCUotFUPrHTgIw38VQEpPPAGANHhssJ7yUVN_fvt2BcrdgJF5cslsgz9AEyvdqNIBnGDl_2b1pFGJO7fL8ZCRaHo_enWElexNM4Gxz_lVohhU6Eg2Fh2b-dwDGJfJ3sY-tob1vsFlkuHsZ0Q-Kedhb4W120N9dukAfAcnGSt9vfSO75LhhwpqXb-YS9cdukBiXUOkP2Rs6Guw";
        final DecodedJWT decoded = JWT.decode(keycloakToken);
        final Builder jwtBuilder = Jwt.withTokenValue(keycloakToken);

        List.of("alg", "typ", "kid").forEach(key -> jwtBuilder.header(key, decoded.getHeaderClaim(key).asString()));

        final Jwt jwtToken = jwtBuilder.claim("azp", decoded.getClaim("azp").asString())
                .claim("realm_access", decoded.getClaim("realm_access").asMap())
                .claim("resource_access", decoded.getClaim("resource_access").asMap()).build();

        final Collection<GrantedAuthority> authorities = converter.convert(jwtToken);

        final List<String> realmRoles = (List<String>) decoded.getClaim("realm_access").asMap().get("roles");
        final List<String> ressourceAccessRoles = (List<String>) ((Map<String, Object>) decoded.getClaim("resource_access").asMap().get(jwtToken.getClaim("azp"))).get("roles");
        assertTrue(CollectionUtils.isNotEmpty(authorities));
        assertEquals(realmRoles.size() + ressourceAccessRoles.size(), authorities.size());
        final List<String> authoritiesAsStringList = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        realmRoles.forEach(role -> assertTrue(authoritiesAsStringList.contains(Roles.ROLE_PREFIX + role)));
        ressourceAccessRoles.forEach(role -> assertTrue(authoritiesAsStringList.contains(Roles.ROLE_PREFIX + role)));
    }
}
