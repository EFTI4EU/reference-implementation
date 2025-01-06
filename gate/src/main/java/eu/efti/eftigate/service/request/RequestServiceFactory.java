package eu.efti.eftigate.service.request;

import eu.efti.commons.enums.RequestTypeEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class RequestServiceFactory {

    private final List<RequestService<?>> requestServices;

    @SuppressWarnings("rawtypes")
    public RequestService getRequestServiceByRequestType(final RequestTypeEnum requestType) {
        return requestServices.stream()
                .filter(requestService -> requestService.supports(requestType)).findFirst()
                .orElse(null);
    }

    @SuppressWarnings("rawtypes")
    public RequestService getRequestServiceByRequestType(final String requestType) {
        return requestServices.stream()
                .filter(requestService -> requestService.supports(requestType)).findFirst()
                .orElse(null);
    }
}
