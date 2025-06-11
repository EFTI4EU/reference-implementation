package eu.efti.eftigate.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class OidcHeaderFilter extends OncePerRequestFilter {

    @Value("${proconnect.allowed.sirets}")
    private List<String> allowedSirets;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String siret = request.getHeader("OIDC_CLAIM_siret");
        if (siret != null && !allowedSirets.isEmpty()
                && !allowedSirets.contains(StringUtils.deleteWhitespace(siret))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "ACCESS DENIED FOR THIS SIRET");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
