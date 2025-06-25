package eu.efti.platformgatesimulator.utils;

import eu.efti.platformgatesimulator.service.AbstractTest;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlatformEftiSchemaUtilsTest extends AbstractTest {

    @Test
    void testCommonToIdentifiers() {
        // Arrange
        SupplyChainConsignment commonConsignment = new SupplyChainConsignment();
        
        // Act
        eu.efti.v1.consignment.identifier.SupplyChainConsignment result = PlatformEftiSchemaUtils.commonToIdentifiers(serializeUtils, commonConsignment);

        // Assert
        assertNotNull(result);
    }
}

