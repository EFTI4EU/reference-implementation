package eu.efti.platformgatesimulator.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
public class SecurityConfig {
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
        http.csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/api/gate-api/**")
                .sessionManagement(
                        management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                .sessionFixation().changeSessionId()
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/gate-api/**").permitAll()
                        .anyRequest().denyAll()
                )
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(final HttpSecurity http, final JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
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
