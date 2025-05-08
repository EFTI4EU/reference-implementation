package eu.efti.eftigate.service;

import eu.efti.commons.utils.MappingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.service.client.DefaultApi;
import eu.efti.eftigate.service.request.ValidationService;
import eu.efti.eftigate.utils.StringAsObjectHttpMessageConverter;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Set;

@AllArgsConstructor
@Service
public class PlatformRestService {
    private static final RestTemplate restTemplate = new RestTemplateBuilder()
            .messageConverters(new StringAsObjectHttpMessageConverter())
            .build();

    private final SerializeUtils serializeUtils;

    private final ValidationService validationService;

    private static DefaultApi createApi(URI restApiBaseUrl) {
        return new DefaultApi(new ApiClient(restTemplate)
                .setBasePath(restApiBaseUrl.toString()));
    }

    public PlatformRestClient getClient(URI restApiBaseUrl) {
        return new PlatformRestClient(createApi(restApiBaseUrl));
    }

    @AllArgsConstructor
    public class PlatformRestClient {
        private final DefaultApi api;

        public SupplyChainConsignment callGetConsignmentSubsets(String datasetId, Set<String> subsetIds) throws PlatformIntegrationServiceException {
            try {
                var xml = (String) api.getConsignmentSubsets(datasetId, subsetIds);
                return serializeUtils.mapXmlStringToJaxbObject(xml, SupplyChainConsignment.class, validationService.getGateSchema());
            } catch (MappingException e) {
                throw new PlatformIntegrationServiceException("Got invalid content from platform", e);
            } catch (HttpClientErrorException e) {
                throw new PlatformIntegrationServiceException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
        }

        public void callPostConsignmentFollowup(String datasetId, String body) throws PlatformIntegrationServiceException {
            try {
                api.postConsignmentFollowup(datasetId, body);
            } catch (HttpClientErrorException e) {
                throw new PlatformIntegrationServiceException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
        }
    }
}
