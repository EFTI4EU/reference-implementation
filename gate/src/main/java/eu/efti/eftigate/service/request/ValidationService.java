package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.v1.edelivery.Request;
import eu.efti.v1.edelivery.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static eu.efti.commons.enums.RequestStatusEnum.ERROR;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
@Getter
public class ValidationService {

    public boolean isRequestValidator(final Request request) {
        return request.getRequestId() != null;
    }

    public boolean isResponseValidator(final Response response) {
        return response.getRequestId() != null;
    }

}
