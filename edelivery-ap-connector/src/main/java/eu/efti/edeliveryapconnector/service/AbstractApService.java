package eu.efti.edeliveryapconnector.service;

import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.domibus.plugin.ws.client.WebserviceClient;
import eu.domibus.plugin.ws.generated.WebServicePluginInterface;

import java.net.MalformedURLException;

public abstract class AbstractApService {

    protected WebServicePluginInterface initApWebService(final ApConfigDto apConfigDto) throws MalformedURLException {
        final WebserviceClient webService = new WebserviceClient(apConfigDto.getUrl(), true);
        return webService.getPort(apConfigDto.getUsername(), apConfigDto.getPassword());
    }
}
