package eu.efti.eftigate.service.request;

import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.constant.EftiGateConstants;
import eu.efti.eftigate.dto.RequestDto;
import eu.efti.eftigate.exception.TechnicalException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
@AllArgsConstructor
public class RequestToEDeliveryActionFunction implements Function<RequestDto, EDeliveryAction> {

    private final GateProperties gateProperties;

    @Override
    public EDeliveryAction apply(final RequestDto requestDto) {
        final RequestTypeEnum requestType = requestDto.getControl().getRequestType();
        if (requestType == null) {
            throw new TechnicalException("Empty Request type for requestId " + requestDto.getId());
        }
        if (EftiGateConstants.IDENTIFIERS_TYPES.contains(requestType)) {
            return EDeliveryAction.GET_IDENTIFIERS;
        } else if (EftiGateConstants.UIL_TYPES.contains(requestType)) {
            return getEDeliveryActionForGateToGateOrGetUil(requestType, requestDto);
        } else return null;
    }

    private EDeliveryAction getEDeliveryActionForGateToGateOrGetUil(final RequestTypeEnum requestType, final RequestDto requestDto) {
        return switch (requestType) {
            case LOCAL_UIL_SEARCH -> EDeliveryAction.GET_UIL;
            case EXTERNAL_ASK_UIL_SEARCH -> gateProperties.isCurrentGate(requestDto.getGateUrlDest()) ? EDeliveryAction.GET_UIL : EDeliveryAction.FORWARD_UIL;
            case EXTERNAL_UIL_SEARCH -> EDeliveryAction.FORWARD_UIL;
            default -> throw new TechnicalException("Empty Request type for requestId " + requestDto.getId());
        };
    }
}
