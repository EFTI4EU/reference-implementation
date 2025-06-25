package eu.efti.eftigate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.xml.sax.SAXParseException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void before() {
        validationService = new ValidationService();
        ReflectionTestUtils.setField(validationService, "gateXsd", "classpath:xsd/edelivery/gate.xsd");
    }

    @ParameterizedTest
    @MethodSource("provideValidXml")
    void validateXml_ShouldNotThrowExceptionWhenXmlAreValid(final String inputValidXml) {
        assertDoesNotThrow(() -> validationService.validateXml(inputValidXml));
    }

    @Test
    @WithMockUser
    void testNotValidPostFollowUpRequest() {
        String body = """
                <postFollowUpRequest xmlns="http://efti.eu/v1/edelivery" xmlns:ns2="http://efti.eu/v1/consignment/identifier">
                	<uil>
                		<gateId>gate</gateId>
                		<platformId>acme</platformId>
                		<datasetId>uuid</datasetId>
                	</uil>
                	<message>The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear</message>
                "</postFollowUpRequest>
                """;

        assertThrows(SAXParseException.class, () -> validationService.validateXml(body));
    }

    public static Stream<Arguments> provideValidXml() {
        return Stream.of(
                Arguments.of("""
                                <identifierQuery xmlns="http://efti.eu/v1/edelivery" xmlns:ns2="http://efti.eu/v1/consignment/identifier" requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                                	<identifier>AA123VV</identifier>
                                	<registrationCountryCode>BE</registrationCountryCode>
                                	<modeCode>1</modeCode>
                                </identifierQuery>
                        """),

                Arguments.of("""
                        <uilQuery xmlns="http://efti.eu/v1/edelivery" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd" requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                        	<uil>
                        		<gateId>FI1</gateId>
                        		<platformId>xxx</platformId>
                        		<datasetId>asdf</datasetId>
                        	</uil>
                        	<subsetId>FI</subsetId>
                        </uilQuery>
                        """),
                Arguments.of("""
                                <ed:identifierResponse xmlns="http://efti.eu/v1/consignment/identifier" xmlns:ed="http://efti.eu/v1/edelivery" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd" status="200">
                        	<ed:consignment>
                        		<!-- eFTI39 -->
                        		<carrierAcceptanceDateTime>202401010000</carrierAcceptanceDateTime>
                        		<deliveryEvent>
                        			<!-- eFTI188 -->
                        			<actualOccurrenceDateTime>202401020000</actualOccurrenceDateTime>
                        		</deliveryEvent>
                        		<mainCarriageTransportMovement>
                        			<!-- eFTI1451 -->
                        			<dangerousGoodsIndicator>false</dangerousGoodsIndicator>
                        			<!-- eFTI581 -->
                        			<modeCode>3</modeCode>
                        			<usedTransportMeans>
                        				<!-- eFTI618 -->
                        				<id>313</id>
                        				<!-- eFTI620 -->
                        				<registrationCountry>
                        					<code>FI</code>
                        				</registrationCountry>
                        			</usedTransportMeans>
                        		</mainCarriageTransportMovement>
                        		<usedTransportEquipment>
                        			<carriedTransportEquipment>
                        				<!-- eFTI448 -->
                        				<id>313</id>
                        				<!-- eFTI1000 -->
                        				<sequenceNumber>1</sequenceNumber>
                        			</carriedTransportEquipment>
                        			<!-- eFTI378 -->
                        			<categoryCode>AE</categoryCode>
                        			<!-- eFTI374 -->
                        			<id>313</id>
                        			<!-- eFTI578 -->
                        			<registrationCountry>
                        				<code>FI</code>
                        			</registrationCountry>
                        			<!-- eFTI987 -->
                        			<sequenceNumber>1</sequenceNumber>
                        		</usedTransportEquipment>
                        		<ed:uil>
                        			<ed:gateId>FI</ed:gateId>
                        			<ed:platformId>1234</ed:platformId>
                        			<ed:datasetId>XXX</ed:datasetId>
                        		</ed:uil>
                        	</ed:consignment>
                        	<ed:consignment>
                        		<!-- eFTI39 -->
                        		<carrierAcceptanceDateTime>202402010000</carrierAcceptanceDateTime>
                        		<deliveryEvent>
                        			<!-- eFTI188 -->
                        			<actualOccurrenceDateTime>202402020000+0000</actualOccurrenceDateTime>
                        		</deliveryEvent>
                        		<mainCarriageTransportMovement>
                        			<!-- eFTI1451 -->
                        			<dangerousGoodsIndicator>false</dangerousGoodsIndicator>
                        			<!-- eFTI581 -->
                        			<modeCode>3</modeCode>
                        			<usedTransportMeans>
                        				<!-- eFTI618 -->
                        				<id>222</id>
                        				<!-- eFTI620 -->
                        				<registrationCountry>
                        					<code>FI</code>
                        				</registrationCountry>
                        			</usedTransportMeans>
                        		</mainCarriageTransportMovement>
                        		<usedTransportEquipment>
                        			<!-- eFTI378 -->
                        			<categoryCode>AE</categoryCode>
                        			<!-- eFTI374 -->
                        			<id>111</id>
                        			<!-- eFTI578 -->
                        			<registrationCountry>
                        				<code>FI</code>
                        			</registrationCountry>
                        			<!-- eFTI987 -->
                        			<sequenceNumber>1</sequenceNumber>
                        		</usedTransportEquipment>
                        		<ed:uil>
                        			<ed:gateId>FI</ed:gateId>
                        			<ed:platformId>1234</ed:platformId>
                        			<ed:datasetId>YYY</ed:datasetId>
                        		</ed:uil>
                        	</ed:consignment>
                        </ed:identifierResponse>
                        """),
                Arguments.of("""
                        <postFollowUpRequest requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c" xmlns="http://efti.eu/v1/edelivery" xmlns:ns2="http://efti.eu/v1/consignment/identifier">
                        	<uil>
                        		<gateId>gate</gateId>
                        		<platformId>acme</platformId>
                        		<datasetId>uuid</datasetId>
                        	</uil>
                        	<message>The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear</message>
                        </postFollowUpRequest>
                        """),
                Arguments.of("<uilResponse xmlns=\"http://efti.eu/v1/edelivery\" xmlns:ns2=\"http://efti.eu/v1/consignment/common\" xmlns:ns3=\"http://efti.eu/v1/consignment/identifier\" requestId=\"67fe38bd-6bf7-4b06-b20e-206264bd639c\" status=\"200\"/>\n")
        );
    }

}
