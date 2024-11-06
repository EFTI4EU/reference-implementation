package eu.efti.identifiersregistry;

import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.codes.TransportEquipmentCategoryCode;
import eu.efti.v1.consignment.identifier.AssociatedTransportEquipment;
import eu.efti.v1.consignment.identifier.LogisticsTransportEquipment;
import eu.efti.v1.consignment.identifier.LogisticsTransportMeans;
import eu.efti.v1.consignment.identifier.LogisticsTransportMovement;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.consignment.identifier.TradeCountry;
import eu.efti.v1.consignment.identifier.TransportEvent;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import eu.efti.v1.types.DateTime;
import eu.efti.v1.types.Identifier17;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifiersMapperTest {
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

        IdentifiersMapper identifiersMapper = new IdentifiersMapper(new ModelMapper());
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

        IdentifiersMapper identifiersMapper = new IdentifiersMapper(new ModelMapper());
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
