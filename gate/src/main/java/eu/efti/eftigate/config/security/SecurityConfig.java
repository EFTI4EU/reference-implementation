package eu.efti.eftigate.config.security;

import eu.efti.eftigate.config.security.converters.KeycloakResourceRolesConverter;
import eu.efti.eftigate.config.security.converters.ProconnectResourceRolesConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.issuers}")
    List<String> issuers;

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakResourceRolesConverter keycloakResourceRolesConverter, ProconnectResourceRolesConverter proconnectResourceRolesConverter) {
        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(keycloakResourceRolesConverter, proconnectResourceRolesConverter));
        return jwtAuthenticationConverter;
    }

    @Bean
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
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(issuerAuthenticationManagerResolver(issuers, jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver issuerAuthenticationManagerResolver(final List<String> issuers, final JwtAuthenticationConverter jwtAuthenticationConverter) {
        final Map<String, AuthenticationManager> authenticationProviderByIssuer = new HashMap<>();
        issuers.forEach(issuer -> {
            final JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(
                    JwtDecoders.fromIssuerLocation(issuer));
            authenticationProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
            authenticationProviderByIssuer.put(issuer, authenticationProvider::authenticate);
        });
        return new JwtIssuerAuthenticationManagerResolver(authenticationProviderByIssuer::get);
    }
}
