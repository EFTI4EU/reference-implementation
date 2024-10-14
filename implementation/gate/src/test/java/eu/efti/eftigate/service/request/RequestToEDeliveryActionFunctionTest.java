package eu.efti.eftigate.service.request;


import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.LOCAL_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.NOTE_SEND;
import static eu.efti.commons.enums.StatusEnum.COMPLETE;
import static eu.efti.commons.enums.StatusEnum.PENDING;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RequestToEDeliveryActionFunctionTest {


    public static final String FR_GATE_URL = "https://efti.gate.fr.eu";
    public static final String DE_GATE_URL = "https://efti.gate.de.eu";

    @ParameterizedTest
    @MethodSource("provideRequests")
    void shouldGetEDeliveryActionFromRequest(final RabbitRequestDto actual, final EDeliveryAction expected) {
        final GateProperties gateProperties = GateProperties.builder().owner(FR_GATE_URL).ap(GateProperties.ApConfig.builder().url(FR_GATE_URL).build()).build();
        final RequestToEDeliveryActionFunction requestToEDeliveryActionFunction = new RequestToEDeliveryActionFunction(gateProperties);

        final EDeliveryAction applied = requestToEDeliveryActionFunction.apply(actual);

        Assertions.assertEquals(expected, applied);
    }

    @Test
    void shouldThrowException_whenGettingEdeliveryActionFromRequestAndRequestTypeIsNull() {
        final GateProperties gateProperties = GateProperties.builder().owner(FR_GATE_URL).ap(GateProperties.ApConfig.builder().url(FR_GATE_URL).build()).build();
        final RequestToEDeliveryActionFunction requestToEDeliveryActionFunction = new RequestToEDeliveryActionFunction(gateProperties);
        final RabbitRequestDto rabbitRequestDto = buildRequestDto(null, FR_GATE_URL, PENDING);
        assertThrows(TechnicalException.class, () -> requestToEDeliveryActionFunction.apply(rabbitRequestDto));
    }

    static Stream<Arguments> provideRequests() {
        return Stream.of(
                arguments(buildRequestDto(LOCAL_IDENTIFIERS_SEARCH, FR_GATE_URL, PENDING), EDeliveryAction.GET_IDENTIFIERS),
                arguments(buildRequestDto(EXTERNAL_IDENTIFIERS_SEARCH, FR_GATE_URL, PENDING), EDeliveryAction.GET_IDENTIFIERS),
                arguments(buildRequestDto(EXTERNAL_ASK_IDENTIFIERS_SEARCH, FR_GATE_URL, PENDING), EDeliveryAction.GET_IDENTIFIERS),
                arguments(buildRequestDto(LOCAL_UIL_SEARCH, FR_GATE_URL, PENDING), EDeliveryAction.GET_UIL),
                arguments(buildRequestDto(EXTERNAL_UIL_SEARCH, FR_GATE_URL, PENDING), EDeliveryAction.FORWARD_UIL),
                arguments(buildRequestDto(EXTERNAL_ASK_UIL_SEARCH, FR_GATE_URL, PENDING), EDeliveryAction.FORWARD_UIL),
                arguments(buildRequestDto(EXTERNAL_ASK_UIL_SEARCH, DE_GATE_URL, PENDING), EDeliveryAction.FORWARD_UIL),
                arguments(buildRequestDto(EXTERNAL_ASK_UIL_SEARCH, FR_GATE_URL, COMPLETE), EDeliveryAction.FORWARD_UIL),
                arguments(buildRequestDto(NOTE_SEND, FR_GATE_URL, PENDING), null)
                );
    }

    private static RabbitRequestDto buildRequestDto(final RequestTypeEnum requestType, final String eftiGateUrl, final StatusEnum controlStatus) {
        return RabbitRequestDto.builder()
                .control(ControlDto.builder().requestType(requestType).eftiGateUrl(eftiGateUrl).status(controlStatus).build())
                .build();
    }
}
