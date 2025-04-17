package eu.efti.eftigate.config.security.converters;

import eu.efti.eftigate.config.security.Roles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Pro Connect roles converter
 */
@Component
public class ProconnectResourceRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Value("${spring.security.oauth2.resourceserver.issuers}")
    List<String> issuers;

    @Override
    public Collection<GrantedAuthority> convert(@NonNull final Jwt jwt) {
        if (canConvert(jwt)) {
            final List<GrantedAuthority> authorities = new ArrayList<>();
            addProConnectRoles(jwt, authorities);
            return authorities;
        }
        return new ArrayList<>();
    }

    private void addProConnectRoles(Jwt jwt, List<GrantedAuthority> authorities) {
        authorities.add(new SimpleGrantedAuthority(Roles.ROLE_ROAD_CONTROLER));
    }

    private boolean canConvert(final Jwt jwt) {
        String keyCloackIssuerUri = issuers.get(1);
        String issuerClaimAsString = jwt.getClaimAsString("iss");
        return issuerClaimAsString.equalsIgnoreCase(keyCloackIssuerUri);
    }
}
