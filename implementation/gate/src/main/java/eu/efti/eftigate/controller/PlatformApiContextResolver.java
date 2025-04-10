package eu.efti.eftigate.controller;

import eu.efti.eftigate.config.security.RestApiRoles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.stream.Collectors;

public class PlatformApiContextResolver {
    public record PlatformContext(String platformId, String role) {
    }

    /**
     * Get details of the authenticated platform user from {@link org.springframework.security.core.context.SecurityContext}.
     *
     * @return details of the user
     * @throws IllegalStateException if no authenticated platform user was found
     */
    public static PlatformContext getPlatformContextOrFail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found");
        }

        var user = (User) authentication.getPrincipal();

        var expectedRoleName = "ROLE_" + RestApiRoles.ROLE_PLATFORM;
        var hasRole = user.getAuthorities().stream().anyMatch(gr -> expectedRoleName.equals(gr.getAuthority()));
        if (hasRole) {
            return new PlatformContext(user.getUsername(), RestApiRoles.ROLE_PLATFORM);
        } else {
            throw new IllegalStateException("Current user does not have role " + expectedRoleName + " but " +
                    user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));
        }
    }
}
