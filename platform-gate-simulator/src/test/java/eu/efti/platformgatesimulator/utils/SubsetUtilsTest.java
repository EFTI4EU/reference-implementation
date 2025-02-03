package eu.efti.platformgatesimulator.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class SubsetUtilsTest {

    @Test
    void parseBySubsetsWithSubsetTest() {

        String xmlString = """
                <consignment xmlns="http://efti.eu/v1/consignment/common"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://efti.eu/v1/consignment/common ../consignment-common.xsd">
                             <applicableServiceCharge>
                                 <appliedAmount currencyId="MRU">1000.000000000000</appliedAmount>
                                 <calculationBasisCode>token</calculationBasisCode>
                                 <calculationBasisPrice>
                                   <basisQuantity>100</basisQuantity>
                                   <categoryTypeCode>token</categoryTypeCode>
                                   <unitAmount currencyId="SOS">1000.000000000000</unitAmount>
                                 </calculationBasisPrice>
                                 <id>token</id>
                                 <payingPartyRoleCode>token</payingPartyRoleCode>
                                 <paymentArrangementCode>token</paymentArrangementCode>
                               </applicableServiceCharge>
                </consignment>
                """;

        Optional<String> result = SubsetUtils.parseBySubsets(xmlString, List.of("AT07"));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().length() < xmlString.length());
        Assertions.assertFalse(result.get().contains("applicableServiceCharge"));
    }
}
