package eu.efti.eftigate.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void before() {
        validationService = new ValidationService();
    }

    @Test
    void isXmlValideIdentifierQuery() {
        String body = """
                                   <identifierQuery xmlns="http://efti.eu/v1/edelivery" xmlns:ns2="http://efti.eu/v1/consignment/identifier" requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                
                    <identifier>AA123VV</identifier>
                
                    <registrationCountryCode>BE</registrationCountryCode>
                
                    <modeCode>1</modeCode>
               
                </identifierQuery>""";

        Optional<String> result = validationService.isXmlValid(body);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void isXmlValideUILQuery() {
        String body = """
                                 <uilQuery
                    xmlns="http://efti.eu/v1/edelivery"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"
                   requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c">
                
                  <uil>
                
                    <gateId>FI1</gateId>
                
                    <platformId>xxx</platformId>
                
                    <datasetId>asdf</datasetId>
                
                  </uil>
                
                  <subsetId>FI</subsetId>
                
                </uilQuery>""";

        Optional<String> result = validationService.isXmlValid(body);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void isXmlValideIdentifierResponse() {
        String body = """
                                <ed:identifierResponse
                 xmlns="http://efti.eu/v1/consignment/identifier"               
                 xmlns:ed="http://efti.eu/v1/edelivery"     
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"           
                 xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"              
                 status="200">
                
                               
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
                
                               
                 </ed:identifierResponse>""";

        Optional<String> result = validationService.isXmlValid(body);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void isXmlValideUilResponse() {
        String body = "<uilResponse xmlns=\"http://efti.eu/v1/edelivery\" xmlns:ns2=\"http://efti.eu/v1/consignment/common\" xmlns:ns3=\"http://efti.eu/v1/consignment/identifier\" requestId=\"67fe38bd-6bf7-4b06-b20e-206264bd639c\" status=\"200\"/>\n";

        Optional<String> result = validationService.isXmlValid(body);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void isXmlValidePostFollowUpRequest() {
        String body = """
                                  <postFollowUpRequest requestId="67fe38bd-6bf7-4b06-b20e-206264bd639c" xmlns="http://efti.eu/v1/edelivery"
                
                                  xmlns:ns2="http://efti.eu/v1/consignment/identifier">
                
                 <uil>
                
                     <gateId>gate</gateId>
                
                     <platformId>acme</platformId>
                
                     <datasetId>uuid</datasetId>
                
                 </uil>
                
                 <message>The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear</message>
                
                </postFollowUpRequest>""";

        Optional<String> result = validationService.isXmlValid(body);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void isXmlValideNotValidPostFollowUpRequest() {
        String body = """
                                  <postFollowUpRequest xmlns="http://efti.eu/v1/edelivery"
                
                                  xmlns:ns2="http://efti.eu/v1/consignment/identifier">
                
                 <uil>
                
                     <gateId>gate</gateId>
                
                     <platformId>acme</platformId>
                
                     <datasetId>uuid</datasetId>
                
                 </uil>
                
                 <message>The inspection did not reveal any anomalies. We recommend that you replace the tires as they are on the verge of wear</message>
                
                "</postFollowUpRequest>""";

        Optional<String> result = validationService.isXmlValid(body);

        Assertions.assertTrue(result.isPresent());
    }


}
