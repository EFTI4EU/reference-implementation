package eu.efti.testsupport;

import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.codes.TransportEquipmentCategoryCode;
import eu.efti.v1.consignment.common.TradeParty;
import eu.efti.v1.types.DateTime;
import eu.efti.v1.types.Identifier17;

import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static eu.efti.testsupport.TestData.random;
import static eu.efti.testsupport.TestData.randomBoolean;
import static eu.efti.testsupport.TestData.randomFutureInstant;
import static eu.efti.testsupport.TestData.randomIdentifier;
import static eu.efti.testsupport.TestData.randomPastInstant;

public class EntityFactory {
    public static eu.efti.v1.consignment.common.SupplyChainConsignment newConsignmentCommon() {
        eu.efti.v1.consignment.common.SupplyChainConsignment c = new eu.efti.v1.consignment.common.SupplyChainConsignment();

        c.setCarrier(new TradeParty());
        c.getCarrier().setId(newIdentifier17());

        c.setCarrierAcceptanceDateTime(newDateTime(randomPastInstant()));

        c.setDeliveryEvent(new  eu.efti.v1.consignment.common.TransportEvent());
        c.getDeliveryEvent().setActualOccurrenceDateTime(newDateTime(randomFutureInstant()));

        var ltmo = new  eu.efti.v1.consignment.common.LogisticsTransportMovement();
        ltmo.setDangerousGoodsIndicator(randomBoolean());
        ltmo.setModeCode(random("1", "2", "3", "4"));
        ltmo.setUsedTransportMeans(new eu.efti.v1.consignment.common.LogisticsTransportMeans());
        ltmo.getUsedTransportMeans().setId(newIdentifier17());
        ltmo.getUsedTransportMeans().setRegistrationCountry(newCommonTradeCountry());
        c.getMainCarriageTransportMovement().add(ltmo);

        var lte = new eu.efti.v1.consignment.common.LogisticsTransportEquipment();
        lte.setId(newIdentifier17());
        lte.setCategoryCode(random(TransportEquipmentCategoryCode.class));
        lte.setRegistrationCountry(newCommonTradeCountry());
        lte.setSequenceNumber(BigInteger.ONE);
        c.getUsedTransportEquipment().add(lte);

        return c;
    }

    public static eu.efti.v1.consignment.identifier.SupplyChainConsignment newConsignmentIdentifier() {
        eu.efti.v1.consignment.identifier.SupplyChainConsignment c = new eu.efti.v1.consignment.identifier.SupplyChainConsignment();

        c.setCarrierAcceptanceDateTime(newDateTime(randomPastInstant()));

        c.setDeliveryEvent(new  eu.efti.v1.consignment.identifier.TransportEvent());
        c.getDeliveryEvent().setActualOccurrenceDateTime(newDateTime(randomFutureInstant()));

        var ltmo = new  eu.efti.v1.consignment.identifier.LogisticsTransportMovement();
        ltmo.setDangerousGoodsIndicator(randomBoolean());
        ltmo.setModeCode(random("1", "2", "3", "4"));
        ltmo.setUsedTransportMeans(new eu.efti.v1.consignment.identifier.LogisticsTransportMeans());
        ltmo.getUsedTransportMeans().setId(newIdentifier17());
        ltmo.getUsedTransportMeans().setRegistrationCountry(newIdentifierTradeCountry());
        c.getMainCarriageTransportMovement().add(ltmo);

        var lte = new eu.efti.v1.consignment.identifier.LogisticsTransportEquipment();
        lte.setId(newIdentifier17());
        lte.setCategoryCode(random(TransportEquipmentCategoryCode.class));
        lte.setRegistrationCountry(newIdentifierTradeCountry());
        lte.setSequenceNumber(BigInteger.ONE);
        c.getUsedTransportEquipment().add(lte);

        return c;
    }

    private static eu.efti.v1.consignment.identifier.TradeCountry newIdentifierTradeCountry() {
        var tc = new eu.efti.v1.consignment.identifier.TradeCountry();
        tc.setCode(random(CountryCode.class));
        return tc;
    }

    private static eu.efti.v1.consignment.common.TradeCountry newCommonTradeCountry() {
        var tc = new eu.efti.v1.consignment.common.TradeCountry();
        tc.setCode(random(CountryCode.class));
        return tc;
    }

    private static Identifier17 newIdentifier17() {
        var id = new Identifier17();
        id.setValue(randomIdentifier());
        return id;
    }

    private static DateTime newDateTime(Instant instant) {
        var dateTime = new DateTime();
        dateTime.setFormatId("205");
        dateTime.setValue(OffsetDateTime
                .ofInstant(instant.truncatedTo(ChronoUnit.MINUTES), ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmZ")));
        return dateTime;
    }
}
