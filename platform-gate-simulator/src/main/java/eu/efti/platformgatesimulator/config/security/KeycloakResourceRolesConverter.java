package eu.efti.platformgatesimulator.config.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Keycloak realm roles converter
 *
 */
@Component
public class KeycloakResourceRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private static final String RESOURCE_ROLES_BASE_CLAIM_NAME = "resource_access";
    private static final String REALM_ROLES_BASE_CLAIM_NAME = "realm_access";
    private static final String ROLES_CLAIM_NAME = "roles";

    @Override
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        addRealmRoles(jwt, authorities);
        addResourcesRoles(jwt, authorities);
        return authorities;
    }

    /**
     * Add realm roles
     *
     * @param jwt         The parsed jwt token
     * @param authorities The list of authorities to update
     */
    @SuppressWarnings("unchecked")
    private void addRealmRoles(final Jwt jwt, final List<GrantedAuthority> authorities) {
        Optional.ofNullable(jwt).ifPresent(jwtToken -> {
            final Map<String, Object> realmAccess = jwtToken.getClaimAsMap(REALM_ROLES_BASE_CLAIM_NAME);
            List<String> roles = null;
            if (realmAccess != null && (roles = (List<String>) realmAccess.get(ROLES_CLAIM_NAME)) != null) {
                authorities.addAll(roles.stream().map(role -> new SimpleGrantedAuthority(Roles.ROLE_PREFIX + role))
                        .collect(Collectors.toSet()));
            }
        });
    }

    /**
     * Add resources roles
     *
     * @param jwt         The parsed jwt token
     * @param authorities The list of authorities to update
     */
    @SuppressWarnings("unchecked")
    private void addResourcesRoles(final Jwt jwt, final List<GrantedAuthority> authorities) {
        Optional.ofNullable(jwt).ifPresent(jwtToken -> {
            final Map<String, Object> resourcesAccess = jwtToken.getClaimAsMap(RESOURCE_ROLES_BASE_CLAIM_NAME);
            String resourceId = jwtToken.getClaimAsString("azp");
            Map<String, Object> resource = null;
            Collection<String> resourceRoles = null;
            if (resourcesAccess != null
                    && (StringUtils.isNotBlank((resourceId))
                            && (resource = (Map<String, Object>) resourcesAccess.get(resourceId)) != null)
                    && (resourceRoles = (Collection<String>) resource.get(ROLES_CLAIM_NAME)) != null) {
                authorities.addAll(resourceRoles.stream().map(role -> new SimpleGrantedAuthority(Roles.ROLE_PREFIX + role))
                        .collect(Collectors.toSet()));
            }
        });
    }
}
