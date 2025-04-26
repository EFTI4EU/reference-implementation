package eu.efti.eftigate.integration;

import eu.efti.commons.utils.EftiSchemaUtils;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.testsupport.RestIntegrationTest;
import eu.efti.identifiersregistry.IdentifiersMapper;
import eu.efti.identifiersregistry.repository.IdentifiersRepository;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.edelivery.Consignment;
import eu.efti.v1.edelivery.UIL;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.UUID;

import static eu.efti.testsupport.TestData.randomIdentifier;
import static eu.efti.testsupport.EntityFactory.newConsignmentIdentifier;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PlatformApiIT extends RestIntegrationTest {
    private static final String GATE_ID = "gate-it";

    @Autowired
    private SerializeUtils serializeUtils;

    @Autowired
    private IdentifiersRepository identifiersRepository;

    @Autowired
    private IdentifiersMapper identifiersMapper;

    @Test
    public void whoamiShouldReturnCallingPlatformId() {
        // Arrange
        var platformId = randomIdentifier();

        // Act
        var res = restApiCallerFactory.createAuthenticatedForPlatformApi(platformId).get("/api/platform/v0/whoami", String.class);

        // Assert
        assertAll(
                () -> assertEquals(HttpStatus.OK, res.getStatus()),
                () -> assertEquals(
                        "<whoamiResponse><appId>" + platformId + "</appId><role>PLATFORM</role></whoamiResponse>",
                        res.getResponseBody())
        );
    }

    @Test
    public void invalidIdentifiersShouldBeRejected() {
        // Arrange
        var platformId = randomIdentifier();
        var datasetId = UUID.randomUUID().toString();

        // Act
        var res = restApiCallerFactory.createAuthenticatedForPlatformApi(platformId)
                .put("/api/platform/v0/consignments/" + datasetId, "<some-invalid-element/>", MediaType.APPLICATION_XML, Void.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    public void validIdentifiersShouldBeAddedToGateDatabase() {
        // Arrange
        var platformId = randomIdentifier();
        var platformConsignment = newConsignmentIdentifier();
        var platformXml = serializeUtils.mapDocToXmlString(EftiSchemaUtils.mapIdentifiersObjectToDoc(serializeUtils, platformConsignment), true);
        var datasetId = UUID.randomUUID().toString();

        // Act
        var res = restApiCallerFactory.createAuthenticatedForPlatformApi(platformId)
                .put("/api/platform/v0/consignments/" + datasetId, platformXml, MediaType.APPLICATION_XML, Void.class);

        // Assert
        var gateConsignment = toConsignmentIdentifier(identifiersRepository.findByUil(GATE_ID, datasetId, platformId).get());
        var gateXml = serializeUtils.mapDocToXmlString(EftiSchemaUtils.mapIdentifiersObjectToDoc(serializeUtils, gateConsignment), true);

        assertAll(
                () -> assertEquals(HttpStatus.OK, res.getStatus()),
                () -> assertEquals(platformXml, gateXml)
        );
    }

    @Test
    public void validIdentifiersShouldBeUpdatedToGateDatabase() {
        // Arrange
        var platformId = randomIdentifier();
        var datasetId = UUID.randomUUID().toString();
        identifiersRepository.save(toEntity(GATE_ID, platformId, datasetId, newConsignmentIdentifier()));
        var originalConsignment = toConsignmentIdentifier(identifiersRepository.findByUil(GATE_ID, datasetId, platformId).get());

        // Act
        var updatedPlatformXml = serializeUtils.mapDocToXmlString(EftiSchemaUtils.mapIdentifiersObjectToDoc(serializeUtils, newConsignmentIdentifier()), true);
        var res = restApiCallerFactory.createAuthenticatedForPlatformApi(platformId)
                .put("/api/platform/v0/consignments/" + datasetId, updatedPlatformXml, MediaType.APPLICATION_XML, Void.class);

        // Assert
        var gateConsignment = toConsignmentIdentifier(identifiersRepository.findByUil(GATE_ID, datasetId, platformId).get());
        var gateXml = serializeUtils.mapDocToXmlString(EftiSchemaUtils.mapIdentifiersObjectToDoc(serializeUtils, gateConsignment), true);

        assertAll(
                () -> assertEquals(HttpStatus.OK, res.getStatus()),
                () -> assertEquals(updatedPlatformXml, gateXml),
                () -> assertNotEquals(updatedPlatformXml, serializeUtils.mapDocToXmlString(EftiSchemaUtils.mapIdentifiersObjectToDoc(serializeUtils, originalConsignment), true))
        );
    }

    private eu.efti.identifiersregistry.entity.Consignment toEntity(String gateId, String platformId, String datasetId, SupplyChainConsignment consignment) {
        var ce = new Consignment();

        var uil = new UIL();
        uil.setGateId(gateId);
        uil.setPlatformId(platformId);
        uil.setDatasetId(datasetId);

        ce.setUil(uil);
        ce.setDeliveryEvent(consignment.getDeliveryEvent());
        ce.setCarrierAcceptanceDateTime(consignment.getCarrierAcceptanceDateTime());
        ce.getUsedTransportEquipment().addAll(consignment.getUsedTransportEquipment());
        ce.getMainCarriageTransportMovement().addAll(consignment.getMainCarriageTransportMovement());

        return identifiersMapper.eDeliveryToEntity(ce);
    }

    private SupplyChainConsignment toConsignmentIdentifier(eu.efti.identifiersregistry.entity.Consignment entity) {
        eu.efti.v1.edelivery.Consignment edeliveryConsignment = identifiersMapper.entityToEdelivery(entity);

        var ci = new SupplyChainConsignment();
        ci.setDeliveryEvent(edeliveryConsignment.getDeliveryEvent());
        ci.setCarrierAcceptanceDateTime(edeliveryConsignment.getCarrierAcceptanceDateTime());
        ci.getMainCarriageTransportMovement().addAll(edeliveryConsignment.getMainCarriageTransportMovement());
        ci.getUsedTransportEquipment().addAll(edeliveryConsignment.getUsedTransportEquipment());

        return ci;
    }
}
