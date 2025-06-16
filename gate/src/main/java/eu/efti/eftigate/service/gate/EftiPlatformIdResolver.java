package eu.efti.eftigate.service.gate;

import eu.efti.eftigate.repository.PlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class EftiPlatformIdResolver {

    private final PlatformRepository platformRepository;

    public String getCommunicationType(final String platformId) {
        return platformRepository.findCommunicationTypeById(platformId);
    }


    /**
     * Get details of the authenticated platform
     *
     * @return details of the user
     * @throws IllegalStateException if no authenticated platform id was found
     */
    public String getPlatformIdOrFail(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalStateException("No authentication token found");
        }

        String clientId = jwt.getClaimAsString("azp");
        if (StringUtils.isNotBlank(clientId)) {
            return getPlatformId(clientId);
        } else {
            throw new IllegalStateException("No authentication client ID found");
        }
    }

    public String getPlatformUrlByPlatformId(final String platformId) {
        return platformRepository.findUrlById(platformId);
    }

    private String getPlatformId(final String clientId) {
        String platformIdByClientId = platformRepository.findPlatformIdByClientId(clientId);
        if (StringUtils.isBlank(platformIdByClientId)) {
            throw new IllegalStateException("No platform found for client ID " + clientId);
        }
        return platformIdByClientId;
    }
}
