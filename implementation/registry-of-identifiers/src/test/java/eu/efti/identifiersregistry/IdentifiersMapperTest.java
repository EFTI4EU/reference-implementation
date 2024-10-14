package eu.efti.identifiersregistry;

import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.codes.TransportEquipmentCategoryCode;
import eu.efti.v1.consignment.identifier.*;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import eu.efti.v1.types.DateTime;
import eu.efti.v1.types.Identifier17;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

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
}
