package eu.efti.eftigate.service.gate;

import eu.efti.eftigate.repository.PlatformRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class EftiPlatformIdResolverTest {

    @Mock
    private PlatformRepository platformRepository;

    @InjectMocks
    EftiPlatformIdResolver eftiPlatformIdResolver;


    @Test
    void shouldGetPlatformId_whenGivenValidJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "username")
                .claim("azp", "clientId")
                .build();

        Mockito.when(platformRepository.findPlatformIdByClientId("clientId")).thenReturn("acme");

        String platformId = eftiPlatformIdResolver.getPlatformIdOrFail(jwt);

        Assertions.assertEquals("acme", platformId);
    }
}
