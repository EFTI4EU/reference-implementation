package eu.efti.eftigate.service.request;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
@AllArgsConstructor
public class RequestToEDeliveryActionFunction implements Function<RabbitRequestDto, EDeliveryAction> {

    private final GateProperties gateProperties;

    @Override
    public EDeliveryAction apply(final RabbitRequestDto requestDto) {
        final RequestTypeEnum controlRequestType = requestDto.getControl().getRequestType();
        if (controlRequestType == null) {
            throw new TechnicalException("Empty Request type for requestId " + requestDto.getId());
        }
        if (RequestType.NOTE.equals(requestDto.getRequestType())) {
            return EDeliveryAction.SEND_NOTES;
        }
        if (EftiGateConstants.IDENTIFIERS_TYPES.contains(controlRequestType)) {
            return EDeliveryAction.GET_IDENTIFIERS;
        } else if (EftiGateConstants.UIL_TYPES.contains(controlRequestType)) {
            return getEDeliveryActionForGateToGateOrGetUil(controlRequestType, requestDto);
        } else return null;
    }

    private EDeliveryAction getEDeliveryActionForGateToGateOrGetUil(final RequestTypeEnum requestType, final RabbitRequestDto requestDto) {
        return switch (requestType) {
            case LOCAL_UIL_SEARCH -> EDeliveryAction.GET_UIL;
            case EXTERNAL_ASK_UIL_SEARCH -> gateProperties.isCurrentGate(requestDto.getGateUrlDest()) ? EDeliveryAction.GET_UIL : EDeliveryAction.FORWARD_UIL;
            case EXTERNAL_UIL_SEARCH -> EDeliveryAction.FORWARD_UIL;
            default -> throw new TechnicalException("Empty Request type for requestId " + requestDto.getId());
        };
    }
}
