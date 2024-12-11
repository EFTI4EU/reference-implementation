package eu.efti.eftigate.service.request;

import eu.efti.v1.edelivery.Request;
import eu.efti.v1.edelivery.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
@Getter
public class ValidationService {

    public boolean isRequestValid(final Request request) {
        return request.getRequestId() != null;
    }

    public boolean isResponseValid(final Response response) {
        return response.getRequestId() != null;
    }

}
