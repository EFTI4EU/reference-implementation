package eu.efti.eftigate.service;

import eu.efti.commons.utils.MappingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.service.client.DefaultApi;
import eu.efti.eftigate.service.request.ValidationService;
import eu.efti.eftigate.utils.StringAsObjectHttpMessageConverter;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Set;

@AllArgsConstructor
@Service
public class PlatformRestService {
    private static final RestClient restClient = createRestClient();

    private final SerializeUtils serializeUtils;

    private final ValidationService validationService;

    private static RestClient createRestClient() {
        return RestClient.builder()
                .messageConverters(converters -> converters.add(0, new StringAsObjectHttpMessageConverter()))
                .build();
    }

    private static DefaultApi createApi(URI restApiBaseUrl) {
        // TODO EREF-72: include authentication info
        return new DefaultApi(new ApiClient(restClient)
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

        public void callPostConsignmentFollowup(String datasetId, String body, String requestId) throws PlatformIntegrationServiceException {
            try {
                api.postConsignmentFollowup(datasetId, body, requestId);
            } catch (HttpClientErrorException e) {
                throw new PlatformIntegrationServiceException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
        }
    }
}
