package eu.efti.eftigate.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcHeaderFilterTest {

    @InjectMocks
    private OidcHeaderFilter oidcHeaderFilter;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        oidcHeaderFilter = new OidcHeaderFilter();
        Field allowedSirets = OidcHeaderFilter.class.getDeclaredField("allowedSirets");
        allowedSirets.setAccessible(true);
        allowedSirets.set(oidcHeaderFilter, List.of("12002301500031", "12002301500032"));
    }

    @Test
    void shouldAllowRequest_WhenHeaderIsValid() throws Exception {
        when(httpServletRequest.getHeader("OIDC_CLAIM_siret")).thenReturn("12002301500031");

        oidcHeaderFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        verify(filterChain, Mockito.times(1)).doFilter(httpServletRequest, httpServletResponse);
        verify(httpServletResponse, never()).sendError(any(), any());
    }

    @Test
    void shouldBlockRequest_WhenHeaderIsFail() throws Exception {
        when(httpServletRequest.getHeader("OIDC_CLAIM_siret")).thenReturn("12002301500033");

        oidcHeaderFilter.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        verify(httpServletResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "ACCESS DENIED FOR THIS SIRET");
        verify(filterChain, never()).doFilter(any(), any());
    }
}
