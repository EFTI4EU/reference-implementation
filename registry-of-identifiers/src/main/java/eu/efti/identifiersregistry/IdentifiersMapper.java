package eu.efti.identifiersregistry;

import eu.efti.commons.dto.identifiers.ConsignmentDto;
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
import eu.efti.v1.edelivery.UIL;
import eu.efti.v1.types.DateTime;
import eu.efti.v1.types.Identifier17;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IdentifiersMapper {

    private final ModelMapper mapper;

    public ConsignmentDto entityToDto(final Consignment consignmentEntity) {
        return mapper.map(consignmentEntity, ConsignmentDto.class);
    }

    public List<ConsignmentDto> entityToDto(final List<Consignment> consignmentEntityList) {
        return consignmentEntityList.stream().map(this::entityToDto).toList();
    }

    private OffsetDateTime fromDateTime(final DateTime dateTime) {
        if (dateTime == null || StringUtils.isBlank(dateTime.getValue())) return null;
        return switch (dateTime.getFormatId()) {
            case "102" -> {
                LocalDate localDate = LocalDate.parse(dateTime.getValue(), DateTimeFormatter.ofPattern("yyyyMMdd"));
                yield localDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            case "203" -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
                LocalDateTime localDateTime = LocalDateTime.parse(dateTime.getValue(), formatter);
                yield OffsetDateTime.of(localDateTime, ZoneId.of("UTC").getRules().getOffset(localDateTime));
            }
            case "205" -> OffsetDateTime.parse(dateTime.getValue(), DateTimeFormatter.ofPattern("yyyyMMddHHmmZ"));
            default -> throw new UnsupportedOperationException("Unsupported formatId: " + dateTime.getFormatId());
        };
    }

    private DateTime fromOffsetDate(final OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        final DateTime dateTime = new DateTime();
        dateTime.setFormatId("205");
        dateTime.setValue(offsetDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmZ")));
        return dateTime;
    }

    public Consignment eDeliveryToEntity(final SaveIdentifiersRequest request) {
        final Consignment consignment = this.eDeliverySupplyToEntity(request.getConsignment());
        consignment.setDatasetId(request.getDatasetId());
        return consignment;
    }

    public Consignment eDeliveryToEntity(final eu.efti.v1.edelivery.Consignment sourceConsignment) {

        Consignment consignment = eDeliverySupplyToEntity(sourceConsignment);

        consignment.setGateId(sourceConsignment.getUil().getGateId());
        consignment.setPlatformId(sourceConsignment.getUil().getPlatformId());
        consignment.setDatasetId(sourceConsignment.getUil().getDatasetId());

        return consignment;
    }

    public Consignment eDeliverySupplyToEntity(final SupplyChainConsignment sourceConsignment) {
        Consignment consignment = new Consignment();

        consignment.setCarrierAcceptanceDatetime(fromDateTime(sourceConsignment.getCarrierAcceptanceDateTime()));
        if (sourceConsignment.getDeliveryEvent() != null) {
            consignment.setDeliveryEventActualOccurrenceDatetime(fromDateTime(sourceConsignment.getDeliveryEvent().getActualOccurrenceDateTime()));
        }

        consignment.getMainCarriageTransportMovements().addAll(sourceConsignment.getMainCarriageTransportMovement().stream().map(movement -> {
            MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
            mainCarriageTransportMovement.setDangerousGoodsIndicator(movement.isDangerousGoodsIndicator());
            mainCarriageTransportMovement.setModeCode(movement.getModeCode());
            LogisticsTransportMeans usedTransportMeans = movement.getUsedTransportMeans();
            if (usedTransportMeans != null) {
                Identifier17 usedTransportMeansId = usedTransportMeans.getId();
                mainCarriageTransportMovement.setUsedTransportMeansId(usedTransportMeansId.getValue());
                mainCarriageTransportMovement.setSchemeAgencyId(usedTransportMeansId.getSchemeAgencyId());
                TradeCountry usedTransportMeansRegistrationCountry = usedTransportMeans.getRegistrationCountry();
                if (usedTransportMeansRegistrationCountry != null) {
                    mainCarriageTransportMovement.setUsedTransportMeansRegistrationCountry(usedTransportMeansRegistrationCountry.getCode().value());
                }
            }
            mainCarriageTransportMovement.setConsignment(consignment);
            return mainCarriageTransportMovement;
        }).toList());

        consignment.setUsedTransportEquipments(sourceConsignment.getUsedTransportEquipment().stream().map(equipment -> {
            UsedTransportEquipment usedTransportEquipment = toUsedTransportEquipmentEntity(equipment);

            usedTransportEquipment.getCarriedTransportEquipments().addAll(equipment.getCarriedTransportEquipment().stream().map(carriedEquipment -> {
                CarriedTransportEquipment carriedTransportEquipment = new CarriedTransportEquipment();
                carriedTransportEquipment.setEquipmentId(carriedEquipment.getId().getValue());
                carriedTransportEquipment.setSchemeAgencyId(carriedEquipment.getId().getSchemeAgencyId());
                carriedTransportEquipment.setSequenceNumber(carriedEquipment.getSequenceNumber().intValue());
                carriedTransportEquipment.setUsedTransportEquipment(usedTransportEquipment);
                return carriedTransportEquipment;
            }).toList());

            return usedTransportEquipment;
        }).toList());
        return consignment;
    }

    private static UsedTransportEquipment toUsedTransportEquipmentEntity(final LogisticsTransportEquipment equipment) {
        UsedTransportEquipment usedTransportEquipment = new UsedTransportEquipment();
        usedTransportEquipment.setEquipmentId(equipment.getId().getValue());
        usedTransportEquipment.setSchemeAgencyId(equipment.getId().getSchemeAgencyId());
        usedTransportEquipment.setRegistrationCountry(equipment.getRegistrationCountry().getCode().value());
        usedTransportEquipment.setSequenceNumber(equipment.getSequenceNumber().intValue());
        if (equipment.getCategoryCode() != null) {
            usedTransportEquipment.setCategoryCode(equipment.getCategoryCode().value());
        }
        return usedTransportEquipment;
    }

    public eu.efti.v1.edelivery.Consignment entityToEdelivery(final Consignment sourceConsignment) {
        final eu.efti.v1.edelivery.Consignment consignment = new eu.efti.v1.edelivery.Consignment();

        consignment.setUil(buildUil(sourceConsignment));

        consignment.setCarrierAcceptanceDateTime(fromOffsetDate(sourceConsignment.getCarrierAcceptanceDatetime()));
        final TransportEvent transportEvent = new TransportEvent();
        transportEvent.setActualOccurrenceDateTime(fromOffsetDate(sourceConsignment.getDeliveryEventActualOccurrenceDatetime()));
        consignment.setDeliveryEvent(transportEvent);

        consignment.getMainCarriageTransportMovement().addAll(CollectionUtils.emptyIfNull(sourceConsignment.getMainCarriageTransportMovements()).stream().map(movement -> {
            LogisticsTransportMovement logisticsTransportMovement = new LogisticsTransportMovement();
            logisticsTransportMovement.setDangerousGoodsIndicator(movement.isDangerousGoodsIndicator());
            logisticsTransportMovement.setModeCode(String.valueOf(movement.getModeCode()));
            final LogisticsTransportMeans logisticsTransportMeans = new LogisticsTransportMeans();
            logisticsTransportMeans.setId(fromId(movement.getUsedTransportMeansId()));
            logisticsTransportMeans.setRegistrationCountry(fromRegistrationCode(movement.getUsedTransportMeansRegistrationCountry()));
            logisticsTransportMovement.setUsedTransportMeans(logisticsTransportMeans);
            return logisticsTransportMovement;
        }).toList());

        consignment.getUsedTransportEquipment().addAll(CollectionUtils.emptyIfNull(sourceConsignment.getUsedTransportEquipments()).stream().map(equipment -> {
            LogisticsTransportEquipment logisticsTransportEquipment = new LogisticsTransportEquipment();
            logisticsTransportEquipment.setSequenceNumber(BigInteger.valueOf(equipment.getSequenceNumber()));
            logisticsTransportEquipment.setRegistrationCountry(fromRegistrationCode(equipment.getRegistrationCountry()));
            logisticsTransportEquipment.setId(fromId(equipment.getEquipmentId()));
            logisticsTransportEquipment.setCategoryCode(equipment.getCategoryCode() != null ? TransportEquipmentCategoryCode.fromValue(equipment.getCategoryCode()) : null);

            logisticsTransportEquipment.getCarriedTransportEquipment().addAll(CollectionUtils.emptyIfNull(equipment.getCarriedTransportEquipments()).stream().map(carriedEquipment -> {
                AssociatedTransportEquipment associatedTransportEquipment = new AssociatedTransportEquipment();
                associatedTransportEquipment.setId(fromIdAndSchemeAgency(carriedEquipment.getEquipmentId(), carriedEquipment.getSchemeAgencyId()));
                associatedTransportEquipment.setSequenceNumber(BigInteger.valueOf(carriedEquipment.getSequenceNumber()));
                return associatedTransportEquipment;
            }).toList());

            return logisticsTransportEquipment;
        }).toList());
        return consignment;
    }

    private TradeCountry fromRegistrationCode(final String code) {
        final TradeCountry tradeCountry = new TradeCountry();
        tradeCountry.setCode(CountryCode.fromValue(code));
        return tradeCountry;
    }

    private Identifier17 fromId(final String id) {
        final Identifier17 identifier17 = new Identifier17();
        identifier17.setValue(id);
        return identifier17;
    }

    private Identifier17 fromIdAndSchemeAgency(final String id, final String schemeAgencyId) {
        final Identifier17 identifier17 = new Identifier17();
        identifier17.setValue(id);
        identifier17.setSchemeAgencyId(schemeAgencyId);
        return identifier17;
    }

    private UIL buildUil(final Consignment consignment) {
        final UIL uil = new UIL();
        uil.setDatasetId(consignment.getDatasetId());
        uil.setGateId(consignment.getGateId());
        uil.setPlatformId(consignment.getPlatformId());
        return uil;
    }
}
