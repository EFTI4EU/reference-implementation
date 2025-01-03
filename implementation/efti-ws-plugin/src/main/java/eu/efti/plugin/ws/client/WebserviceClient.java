package eu.efti.plugin.ws.client;

import eu.efti.plugin.ws.generated.WebServicePlugin;
import eu.efti.plugin.ws.generated.WebServicePluginInterface;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.soap.SOAPBinding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebserviceClient {

    private final String wsdl;

    private final boolean logMessages;

    public WebserviceClient(String webserviceLocation, boolean logMessages) {
        this.wsdl = webserviceLocation;
        this.logMessages = logMessages;
    }

    public WebServicePluginInterface getPort() throws MalformedURLException {
        return getPort(null, null);
    }

    public WebServicePluginInterface getPort(String username, String password) throws MalformedURLException {
        if (wsdl == null || wsdl.isEmpty()) {
            throw new IllegalArgumentException("No webservice location specified");
        }

        WebServicePlugin backendService = new WebServicePlugin(new URL(wsdl), new QName("http://eu.domibus.wsplugin/", "WebServicePlugin"));
        WebServicePluginInterface backendPort = backendService.getWEBSERVICEPLUGINPORT();

        //enable chunking
        BindingProvider bindingProvider = (BindingProvider) backendPort;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, wsdl);

        if (StringUtils.isNotBlank(username)) {
            log.debug("Adding username [ {} ] to the requestContext", username);
            bindingProvider.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
        }
        if (StringUtils.isNotBlank(password)) {
            log.debug("Adding password to the requestContext");
            bindingProvider.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
        }

        Map<String, Object> ctxt = bindingProvider.getRequestContext();
        ctxt.put("com.sun.xml.ws.transport.http.client.streaming.chunk.size", 8192);
        //enable MTOM
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding();
        binding.setMTOMEnabled(true);

        if (logMessages) {
            List<Handler> handlers = bindingProvider.getBinding().getHandlerChain();
            handlers.add(new MessageLoggingHandler());
            bindingProvider.getBinding().setHandlerChain(handlers);
        }

        return backendPort;
    }
}
