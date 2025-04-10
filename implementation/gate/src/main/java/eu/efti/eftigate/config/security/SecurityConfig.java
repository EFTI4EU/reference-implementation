package eu.efti.eftigate.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.Set;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@EnableWebSecurity
public class SecurityConfig {
    private record Principal(String username, String role) {}

    private static class EftiApiPreAuthenticatedUserHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
        private static final Logger logger = LoggerFactory.getLogger(EftiApiPreAuthenticatedUserHeaderFilter.class);

        @Override
        protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
            var userId = parseHeader(request, "X-Pre-Authenticated-User-Id");
            var role = parseHeader(request, "X-Pre-Authenticated-User-Role");

            if (userId != null && role != null) {
                return new Principal(userId, role);
            } else {
                return null;
            }
        }

        private String parseHeader(HttpServletRequest request, String header) {
            var value = request.getHeader(header);
            if (logger.isDebugEnabled()) {
                logger.debug("{}: {}", header, value);
            }
            return StringUtils.defaultIfBlank(value, null);
        }

        @Override
        protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
            return "not-applicable";
        }
    }

    private static PreAuthenticatedAuthenticationProvider createRestApiPreAuthenticatedAuthenticationProvider() {
        var userDetailsService = new AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>() {
            @Override
            public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
                var principal = (Principal) token.getPrincipal();
                // Note: spring security expects authority names (roles) to have prefix "ROLE_".
                return new User(principal.username, "", Set.of(new SimpleGrantedAuthority("ROLE_" + principal.role)));
            }
        };

        var provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService);

        return provider;
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(final KeycloakResourceRolesConverter keycloakResourceRolesConverter) {
        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(keycloakResourceRolesConverter);
        jwtAuthenticationConverter.setPrincipalClaimName("preferred_username");
        return jwtAuthenticationConverter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain platformApiFilterChain(final HttpSecurity http) throws Exception {
        var eftiApiPreAuthenticatedUserHeaderFilter = new EftiApiPreAuthenticatedUserHeaderFilter();
        eftiApiPreAuthenticatedUserHeaderFilter.setContinueFilterChainOnUnsuccessfulAuthentication(false);
        eftiApiPreAuthenticatedUserHeaderFilter.setAuthenticationManager(new ProviderManager(createRestApiPreAuthenticatedAuthenticationProvider()));

        http.csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/api/platform/**")
                .addFilter(eftiApiPreAuthenticatedUserHeaderFilter)
                .sessionManagement(
                        management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                .sessionFixation().changeSessionId()
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/platform/**").hasRole(RestApiRoles.ROLE_PLATFORM)
                        .anyRequest().denyAll()
                )
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(final HttpSecurity http, final JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                .sessionFixation().changeSessionId()
                )
                .authorizeHttpRequests(authorize -> authorize
                        //open url
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        //require login to everything else
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(customizer -> customizer.jwtAuthenticationConverter((jwtAuthenticationConverter))));

        return http.build();
    }
}
