package eu.efti.platformgatesimulator.mapper;

import eu.efti.v1.codes.CountryCode;
import eu.efti.v1.codes.TransportEquipmentCategoryCode;
import eu.efti.v1.consignment.identifier.LogisticsTransportMeans;
import eu.efti.v1.consignment.identifier.TradeCountry;
import eu.efti.v1.json.CarriedTransportEquipment;
import eu.efti.v1.json.MainCarriageTransportMovement;
import eu.efti.v1.json.UsedTransportEquipment;
import eu.efti.v1.json.UsedTransportMeans;

import java.math.BigInteger;
import java.util.stream.Collectors;

public class MapperUtils {

    public eu.efti.v1.edelivery.SaveIdentifiersRequest mapToEdeliveryRequest(eu.efti.v1.json.SaveIdentifiersRequest sourceSaveIdentifiersRequest) {
        if (sourceSaveIdentifiersRequest == null) {
            return null;
        }
        eu.efti.v1.edelivery.SaveIdentifiersRequest resultSaveIdentifiersRequest = new eu.efti.v1.edelivery.SaveIdentifiersRequest();
        resultSaveIdentifiersRequest.setDatasetId(sourceSaveIdentifiersRequest.getDatasetId());
        resultSaveIdentifiersRequest.setConsignment(from(sourceSaveIdentifiersRequest.getConsignment()));
        return resultSaveIdentifiersRequest;
    }

    public eu.efti.v1.consignment.identifier.SupplyChainConsignment from(eu.efti.v1.json.Consignment sourceConsignment) {
        if (sourceConsignment == null) {
            return null;
        }
        eu.efti.v1.consignment.identifier.SupplyChainConsignment resultSupplyChainConsignment = new eu.efti.v1.consignment.identifier.SupplyChainConsignment();
        resultSupplyChainConsignment.setCarrierAcceptanceDateTime(from(sourceConsignment.getCarrierAcceptanceDateTime()));
        resultSupplyChainConsignment.setDeliveryEvent(from(sourceConsignment.getDeliveryEvent()));
        if (sourceConsignment.getMainCarriageTransportMovement() != null) {
            resultSupplyChainConsignment.getMainCarriageTransportMovement().addAll(sourceConsignment.getMainCarriageTransportMovement().stream().map(this::from).collect(Collectors.toList()));
        }
        if (sourceConsignment.getUsedTransportEquipment() != null) {
            resultSupplyChainConsignment.getUsedTransportEquipment().addAll(sourceConsignment.getUsedTransportEquipment().stream().map(this::from).collect(Collectors.toList()));
        }
        return resultSupplyChainConsignment;
    }

    private eu.efti.v1.consignment.identifier.LogisticsTransportEquipment from(UsedTransportEquipment usedTransportEquipment) {
        if (usedTransportEquipment == null) {
            return null;
        }
        eu.efti.v1.consignment.identifier.LogisticsTransportEquipment resultLogisticsTransportEquipment = new eu.efti.v1.consignment.identifier.LogisticsTransportEquipment();
        resultLogisticsTransportEquipment.setCategoryCode(from(usedTransportEquipment.getCategoryCode()));
        resultLogisticsTransportEquipment.setSequenceNumber(BigInteger.valueOf(usedTransportEquipment.getSequenceNumber()));
        resultLogisticsTransportEquipment.setId(from(usedTransportEquipment.getId()));
        resultLogisticsTransportEquipment.setRegistrationCountry(from(usedTransportEquipment.getRegistrationCountry()));
        if (usedTransportEquipment.getCarriedTransportEquipment() != null) {
            resultLogisticsTransportEquipment.getCarriedTransportEquipment().addAll(usedTransportEquipment.getCarriedTransportEquipment().stream().map(this::from).collect(Collectors.toList()));
        }
        return resultLogisticsTransportEquipment;
    }

    private eu.efti.v1.consignment.identifier.AssociatedTransportEquipment from(CarriedTransportEquipment carriedTransportEquipment) {
        if (carriedTransportEquipment == null) {
            return null;
        }
        eu.efti.v1.consignment.identifier.AssociatedTransportEquipment resultAssociatedTransportEquipment = new eu.efti.v1.consignment.identifier.AssociatedTransportEquipment();
        resultAssociatedTransportEquipment.setSequenceNumber(BigInteger.valueOf(carriedTransportEquipment.getSequenceNumber()));
        resultAssociatedTransportEquipment.setId(from(carriedTransportEquipment.getId()));
        return resultAssociatedTransportEquipment;
    }

    private eu.efti.v1.codes.TransportEquipmentCategoryCode from(UsedTransportEquipment.CategoryCode categoryCode) {
        if (categoryCode == null) {
            return null;
        }
        return TransportEquipmentCategoryCode.fromValue(categoryCode.value());
    }

    private eu.efti.v1.consignment.identifier.LogisticsTransportMovement from(MainCarriageTransportMovement mainCarriageTransportMovement) {
        if (mainCarriageTransportMovement == null) {
            return null;
        }
        eu.efti.v1.consignment.identifier.LogisticsTransportMovement resultLogisticsTransportMovement = new eu.efti.v1.consignment.identifier.LogisticsTransportMovement();
        resultLogisticsTransportMovement.setModeCode(mainCarriageTransportMovement.getModeCode());
        resultLogisticsTransportMovement.setDangerousGoodsIndicator(mainCarriageTransportMovement.getDangerousGoodsIndicator());
        resultLogisticsTransportMovement.setUsedTransportMeans(from(mainCarriageTransportMovement.getUsedTransportMeans()));
        return resultLogisticsTransportMovement;
    }

    private eu.efti.v1.consignment.identifier.LogisticsTransportMeans from(UsedTransportMeans usedTransportMeans) {
        if (usedTransportMeans == null) {
            return null;
        }
        LogisticsTransportMeans resultLogisticsTransportMeans = new LogisticsTransportMeans();
        resultLogisticsTransportMeans.setRegistrationCountry(from(usedTransportMeans.getRegistrationCountry()));
        resultLogisticsTransportMeans.setId(from(usedTransportMeans.getId()));
        return resultLogisticsTransportMeans;
    }

    private eu.efti.v1.types.Identifier17 from(eu.efti.v1.json.Identifier17 id) {
        if (id == null) {
            return null;
        }
        eu.efti.v1.types.Identifier17 resultIdentifier = new eu.efti.v1.types.Identifier17();
        resultIdentifier.setValue(id.getValue());
        resultIdentifier.setSchemeAgencyId(id.getSchemeAgencyId());
        return resultIdentifier;
    }

    private eu.efti.v1.consignment.identifier.TradeCountry from(eu.efti.v1.json.TradeCountry registrationCountry) {
        if (registrationCountry == null) {
            return null;
        }
        TradeCountry tradeCountry = new TradeCountry();
        tradeCountry.setCode(from(registrationCountry.getCode()));
        return tradeCountry;
    }

    private eu.efti.v1.codes.CountryCode from(eu.efti.v1.json.TradeCountry.Code code) {
        if (code == null) {
            return null;
        }
        return CountryCode.valueOf(code.name());
    }

    public eu.efti.v1.consignment.identifier.TransportEvent from(eu.efti.v1.json.DeliveryEvent sourceTransportEvent) {
        if (sourceTransportEvent == null) {
            return null;
        }
        eu.efti.v1.consignment.identifier.TransportEvent resultTransportEvent = new eu.efti.v1.consignment.identifier.TransportEvent();
        resultTransportEvent.setActualOccurrenceDateTime(from(sourceTransportEvent.getActualOccurrenceDateTime()));
        return resultTransportEvent;
    }

    public eu.efti.v1.types.DateTime from(eu.efti.v1.json.DateTime sourceDateTime) {
        if (sourceDateTime == null) {
            return null;
        }
        eu.efti.v1.types.DateTime resultDateTime = new eu.efti.v1.types.DateTime();
        resultDateTime.setFormatId(sourceDateTime.getFormatId());
        resultDateTime.setValue(sourceDateTime.getValue());
        return resultDateTime;
    }

}
