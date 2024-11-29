package eu.efti.identifiersregistry;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.codes.TransportEquipmentCategoryCode;
import eu.efti.v1.consignment.identifier.*;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import eu.efti.v1.edelivery.UIL;
import eu.efti.v1.types.DateTime;
import eu.efti.v1.types.Identifier17;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdentifiersMapperTest {

    private ResourceLoader resourceLoader;

    public static final String XML_FILE_TYPE = "xml";

    private final ModelMapper modelMapper = new ModelMapper();

    private final IdentifiersMapper identifiersMapper = new IdentifiersMapper(modelMapper);

    @BeforeEach
    void before() {
        resourceLoader = new DefaultResourceLoader();
    }

    private Resource tryOpenFile(final String path, final String ext) {
        final String filePath = String.join(".", path, ext);
        return resourceLoader.getResource(filePath);
    }

    @Test
    void entityToEdelivery() {
        UsedTransportEquipment usedTransportEquipment = UsedTransportEquipment.builder()
                .equipmentId("equipementId")
                .id(1)
                .carriedTransportEquipments(List.of(CarriedTransportEquipment.builder().build()))
                .consignment(new Consignment())
                .registrationCountry("FR")
                .schemeAgencyId("idSchemeAgencyId")
                .sequenceNumber(1).build();

        Consignment test = new Consignment();

        test.setDatasetId("datasetId");
        test.setId(1);
        test.setGateId("gateId");
        test.setPlatformId("platformId");
        test.setUsedTransportEquipments(List.of(usedTransportEquipment));
        test.setMainCarriageTransportMovements(List.of(MainCarriageTransportMovement.builder()
                .id(1).consignment(new Consignment()).dangerousGoodsIndicator(false)
                .modeCode((short) 1)
                .usedTransportMeansId("usedTransportMeansId")
                .usedTransportMeansRegistrationCountry("FR")
                .build()));
        test.setCarrierAcceptanceDatetime(OffsetDateTime.now());
        test.setDeliveryEventActualOccurrenceDatetime(OffsetDateTime.now());

        eu.efti.v1.edelivery.Consignment consignment = identifiersMapper.entityToEdelivery(test);

        assertEquals("FR", consignment.getUsedTransportEquipment().get(0).getRegistrationCountry().getCode().name());
        assertEquals("usedTransportMeansId", consignment.getMainCarriageTransportMovement().get(0).getUsedTransportMeans().getId().getValue());
    }

    @Test
    void eDeliveryToEntityTest() {
        DateTime dateTime = new DateTime();
        UIL uil = new UIL();
        uil.setDatasetId("datasetId");
        uil.setGateId("gateId");
        uil.setPlatformId("platformId");
        TransportEvent transportEvent = new TransportEvent();
        transportEvent.setActualOccurrenceDateTime(dateTime);
        eu.efti.v1.edelivery.Consignment test = new eu.efti.v1.edelivery.Consignment();
        test.setUil(uil);
        test.setDeliveryEvent(transportEvent);
        test.setCarrierAcceptanceDateTime(dateTime);

        Consignment consignment = identifiersMapper.eDeliveryToEntity(test);

        assertEquals("datasetId", consignment.getDatasetId());
        assertEquals("gateId", consignment.getGateId());
        assertEquals("platformId", consignment.getPlatformId());
    }

    @Test
    void eDeliverySupplyToEntityTest() throws IOException {
        Resource resource = tryOpenFile("/xml/test", XML_FILE_TYPE);
        SupplyChainConsignment supplyChainConsignment;
        try {
            final Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
            final JAXBElement<eu.efti.v1.consignment.identifier.SupplyChainConsignment> jaxbElement = (JAXBElement<eu.efti.v1.consignment.identifier.SupplyChainConsignment>) unmarshaller.unmarshal(resource.getInputStream());
            supplyChainConsignment = jaxbElement.getValue();
        } catch (JAXBException e) {
            throw new TechnicalException("error while writing content", e);
        }
        Consignment consignment = identifiersMapper.eDeliverySupplyToEntity(supplyChainConsignment);

        assertEquals("2024-01-01T00:00Z", consignment.getCarrierAcceptanceDatetime().toString());
    }

    @Test
    void testMapConsignmentToInternalModel() {
        SaveIdentifiersRequest request = new SaveIdentifiersRequest();
        request.setDatasetId("datasetId");
        SupplyChainConsignment consignment = new SupplyChainConsignment();

        consignment.setCarrierAcceptanceDateTime(dateTimeOf("202107111200+0100", "205"));

        TransportEvent transportEvent = new TransportEvent();
        transportEvent.setActualOccurrenceDateTime(dateTimeOf("20210723", "102"));
        consignment.setDeliveryEvent(transportEvent);
        LogisticsTransportMovement movement = new LogisticsTransportMovement();
        movement.setDangerousGoodsIndicator(true);
        movement.setModeCode("1");
        LogisticsTransportMeans transportMeans = new LogisticsTransportMeans();
        transportMeans.setId(toIdentifier17("123", "UN"));
        transportMeans.setRegistrationCountry(tradeCountryOf(CountryCode.AE));
        movement.setUsedTransportMeans(transportMeans);
        consignment.getMainCarriageTransportMovement().add(movement);
        request.setConsignment(consignment);


        LogisticsTransportEquipment equipment = new LogisticsTransportEquipment();
        equipment.setId(toIdentifier17("123", "UN"));
        equipment.setRegistrationCountry(tradeCountryOf(CountryCode.AE));
        equipment.setSequenceNumber(BigInteger.ONE);
        equipment.setCategoryCode(TransportEquipmentCategoryCode.BPQ);

        // Add CarriedTransportEquipment
        AssociatedTransportEquipment carriedEquipment = new AssociatedTransportEquipment();
        carriedEquipment.setId(toIdentifier17("456", "UN"));
        carriedEquipment.setSequenceNumber(BigInteger.TWO);
        equipment.getCarriedTransportEquipment().add(carriedEquipment);

        request.getConsignment().getUsedTransportEquipment().add(equipment);

        eu.efti.identifiersregistry.entity.Consignment internalConsignment = identifiersMapper.eDeliveryToEntity(request);
        assertEquals("datasetId", internalConsignment.getDatasetId());
        assertEquals(OffsetDateTime.of(2021, 7, 11, 12, 0, 0, 0, ZoneOffset.ofHours(1)), internalConsignment.getCarrierAcceptanceDatetime());
        assertEquals(OffsetDateTime.of(2021, 7, 23, 0, 0, 0, 0, ZoneOffset.UTC), internalConsignment.getDeliveryEventActualOccurrenceDatetime());

        assertEquals(1, internalConsignment.getMainCarriageTransportMovements().size());
        assertEquals(1, internalConsignment.getMainCarriageTransportMovements().get(0).getModeCode());
        assertTrue(internalConsignment.getMainCarriageTransportMovements().get(0).isDangerousGoodsIndicator());
        assertEquals("123", internalConsignment.getMainCarriageTransportMovements().get(0).getUsedTransportMeansId());
        assertEquals("AE", internalConsignment.getMainCarriageTransportMovements().get(0).getUsedTransportMeansRegistrationCountry());

        // check the equipment got mapped
        assertEquals(1, internalConsignment.getUsedTransportEquipments().size());
        assertEquals("123", internalConsignment.getUsedTransportEquipments().get(0).getEquipmentId());
        assertEquals("AE", internalConsignment.getUsedTransportEquipments().get(0).getRegistrationCountry());
        assertEquals(1, internalConsignment.getUsedTransportEquipments().get(0).getSequenceNumber());
        assertEquals(TransportEquipmentCategoryCode.BPQ.value(), internalConsignment.getUsedTransportEquipments().get(0).getCategoryCode());

        // Check that carried equipment got mapped
        assertEquals(1, internalConsignment.getUsedTransportEquipments().get(0).getCarriedTransportEquipments().size());
        assertEquals("456", internalConsignment.getUsedTransportEquipments().get(0).getCarriedTransportEquipments().get(0).getEquipmentId());
        assertEquals(2, internalConsignment.getUsedTransportEquipments().get(0).getCarriedTransportEquipments().get(0).getSequenceNumber());
    }

    private static TradeCountry tradeCountryOf(CountryCode countryCode) {
        TradeCountry tradeCountry = new TradeCountry();

        tradeCountry.setCode(countryCode);
        return tradeCountry;
    }

    private static Identifier17 toIdentifier17(String idString, String schemeAgencyId) {
        Identifier17 id = new Identifier17();
        id.setValue(idString);
        id.setSchemeAgencyId(schemeAgencyId);
        return id;
    }

    private static DateTime dateTimeOf(String dateTimeString, String typeCode) {
        DateTime carrierAcceptanceDateTime = new DateTime();
        carrierAcceptanceDateTime.setValue(dateTimeString);

        carrierAcceptanceDateTime.setFormatId(typeCode);
        return carrierAcceptanceDateTime;
    }

    @Test
    void testMapInternalModelToConsignment() {
        Consignment internalConsignment = new Consignment();
        internalConsignment.setDatasetId("datasetId");
        internalConsignment.setCarrierAcceptanceDatetime(OffsetDateTime.of(2021, 7, 11, 12, 0, 0, 0, ZoneOffset.ofHours(1)));
        internalConsignment.setDeliveryEventActualOccurrenceDatetime(OffsetDateTime.of(2021, 7, 23, 0, 0, 0, 0, ZoneOffset.UTC));

        MainCarriageTransportMovement movement = new MainCarriageTransportMovement();
        movement.setDangerousGoodsIndicator(true);
        movement.setModeCode((short) 1);
        movement.setUsedTransportMeansId("123");
        movement.setUsedTransportMeansRegistrationCountry("AE");
        internalConsignment.getMainCarriageTransportMovements().add(movement);

        UsedTransportEquipment equipment = new UsedTransportEquipment();
        equipment.setEquipmentId("123");
        equipment.setRegistrationCountry("AE");
        equipment.setSequenceNumber(1);
        equipment.setCategoryCode(TransportEquipmentCategoryCode.BPQ.value());

        CarriedTransportEquipment carriedEquipment = new CarriedTransportEquipment();
        carriedEquipment.setEquipmentId("456");
        carriedEquipment.setSequenceNumber(2);
        equipment.getCarriedTransportEquipments().add(carriedEquipment);

        internalConsignment.getUsedTransportEquipments().add(equipment);

        var eDeliveryConsignment = identifiersMapper.entityToEdelivery(internalConsignment);

        assertEquals("datasetId", eDeliveryConsignment.getUil().getDatasetId());
        assertEquals("202107111200+0100", eDeliveryConsignment.getCarrierAcceptanceDateTime().getValue());
        assertEquals("202107230000+0000", eDeliveryConsignment.getDeliveryEvent().getActualOccurrenceDateTime().getValue());

        assertEquals(1, eDeliveryConsignment.getMainCarriageTransportMovement().size());
        assertEquals("1", eDeliveryConsignment.getMainCarriageTransportMovement().get(0).getModeCode());
        assertTrue(eDeliveryConsignment.getMainCarriageTransportMovement().get(0).isDangerousGoodsIndicator());
        assertEquals("123", eDeliveryConsignment.getMainCarriageTransportMovement().get(0).getUsedTransportMeans().getId().getValue());
        assertEquals("AE", eDeliveryConsignment.getMainCarriageTransportMovement().get(0).getUsedTransportMeans().getRegistrationCountry().getCode().value());

        assertEquals(1, eDeliveryConsignment.getUsedTransportEquipment().size());
        LogisticsTransportEquipment theOnlyTransportEquipment = eDeliveryConsignment.getUsedTransportEquipment().get(0);
        assertEquals("123", theOnlyTransportEquipment.getId().getValue());
        assertEquals("AE", theOnlyTransportEquipment.getRegistrationCountry().getCode().value());
        assertEquals(BigInteger.ONE, theOnlyTransportEquipment.getSequenceNumber());
        assertEquals(TransportEquipmentCategoryCode.BPQ, theOnlyTransportEquipment.getCategoryCode());

        assertEquals(1, theOnlyTransportEquipment.getCarriedTransportEquipment().size());
        AssociatedTransportEquipment theOnlyAssociatedEquipment = theOnlyTransportEquipment.getCarriedTransportEquipment().get(0);
        assertEquals("456", theOnlyAssociatedEquipment.getId().getValue());
        assertEquals(BigInteger.TWO, theOnlyAssociatedEquipment.getSequenceNumber());
    }
}
