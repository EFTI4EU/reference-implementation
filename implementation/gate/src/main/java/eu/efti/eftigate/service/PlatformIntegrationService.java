package eu.efti.eftigate.service;

import eu.efti.commons.utils.MappingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.service.client.DefaultApi;
import eu.efti.eftigate.service.request.ValidationService;
import eu.efti.eftigate.utils.StringAsObjectHttpMessageConverter;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PlatformIntegrationService {
    private static final RestTemplate restTemplate = new RestTemplateBuilder()
            .messageConverters(new StringAsObjectHttpMessageConverter())
            .build();

    private final List<GateProperties.PlatformProperties> platformsProperties;

    private final SerializeUtils serializeUtils;

    private final ValidationService validationService;

    private Optional<GateProperties.PlatformProperties> getPlatformProperties(String platformId) {
        return platformsProperties.stream()
                .filter(platformProperties -> platformProperties.platformId().equals(platformId))
                .findFirst();
    }

    private static DefaultApi createApi(GateProperties.PlatformProperties platformProperties) {
        return new DefaultApi(new ApiClient(restTemplate)
                .setBasePath(platformProperties.restApiBaseUrl().toString()));
    }

    @Autowired
    public PlatformIntegrationService(GateProperties gateProperties, SerializeUtils serializeUtils, ValidationService validationService) {
        this.platformsProperties = gateProperties.getPlatforms();
        this.serializeUtils = serializeUtils;
        this.validationService = validationService;
    }

    public boolean platformExists(String platformId) {
        return getPlatformProperties(platformId).isPresent();
    }

    public record PlatformInfo(boolean useRestApi, URI restApiBaseUrl) {
    }

    public Optional<PlatformInfo> getPlatformInfo(String platformId) {
        return getPlatformProperties(platformId)
                .map(platformProperties -> new PlatformInfo(platformProperties.restApiBaseUrl() != null, platformProperties.restApiBaseUrl()));
    }

    public SupplyChainConsignment callGetConsignmentSubsets(String platformId, String datasetId, Set<String> subsetIds) throws PlatformIntegrationServiceException {
        try {
            var xml = (String) getApi(platformId).getConsignmentSubsets(datasetId, subsetIds);
            return serializeUtils.mapXmlStringToJaxbObject(xml, SupplyChainConsignment.class, validationService.getGateSchema());
        } catch (MappingException e) {
            throw new PlatformIntegrationServiceException("Got invalid content from platform", e);
        } catch (HttpClientErrorException e) {
            throw new PlatformIntegrationServiceException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public void callPostConsignmentFollowup(String platformId, String datasetId, String body) throws PlatformIntegrationServiceException {
        try {
            getApi(platformId).postConsignmentFollowup(datasetId, body);
        } catch (HttpClientErrorException e) {
            throw new PlatformIntegrationServiceException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private DefaultApi getApi(String platformId) {
        var properties = getPlatformProperties(platformId)
                .orElseThrow(() ->
                        // Expect the platformId to have been validated earlier before trying to invoke the api
                        new IllegalArgumentException("No configuration for platform: " + platformId));
        return createApi(properties);
    }
}
