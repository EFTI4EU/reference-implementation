package eu.efti.eftigate.service.gate;

import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.eftigate.config.GateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GateToRequestTypeFunctionTest {
    private GateToRequestTypeFunction gateToRequestTypeFunction;

    @BeforeEach
    public void before() {
        final GateProperties gateProperties = GateProperties.builder()
                .owner("https://efti.gate.fr.eu")
                .ap(GateProperties.ApConfig.builder()
                        .url("https://efti.gate.fr.eu")
                        .build()).build();
        gateToRequestTypeFunction = new GateToRequestTypeFunction(gateProperties);
    }

    @Test
    void shouldGetRequestType_WhenMultipleGatesAreGiven(){
        //Act
        final RequestTypeEnum requestType = gateToRequestTypeFunction.apply(List.of("https://efti.gate.fr.eu", "https://efti.gate.be.eu"));

        //Assert
        assertEquals(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH, requestType);
    }

    @Test
    void shouldGetRequestType_WhenSingleGateIsGivenAndIsEqualToLocalGate(){
        //Act
        final RequestTypeEnum requestType = gateToRequestTypeFunction.apply(List.of("https://efti.gate.fr.eu"));

        //Assert
        assertEquals(RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH, requestType);
    }

    @Test
    void shouldGetRequestType_WhenSingleGateIsGivenAndIsDifferentFromLocalGate(){
        //Act
        final RequestTypeEnum requestType = gateToRequestTypeFunction.apply(List.of("https://efti.gate.be.eu"));

        //Assert
        assertEquals(RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH, requestType);
    }


}
