package eu.efti.identifiersregistry;

import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.identifiersregistry.entity.CarriedTransportEquipment;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.MainCarriageTransportMovement;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import eu.efti.v1.types.DateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IdentifiersMapper {

    public IdentifiersDto entityToDto(final Consignment consignmentEntity) {
        IdentifiersDto dto = new IdentifiersDto();
        dto.setEFTIGateUrl(consignmentEntity.getGateId());
        dto.setEFTIPlatformUrl(consignmentEntity.getPlatformId());
        dto.setEFTIDataUuid(consignmentEntity.getDatasetId());
        if (consignmentEntity.getMainCarriageTransportMovements() != null) {
            consignmentEntity.getMainCarriageTransportMovements().forEach(mainCarriageTransportMovement ->
                    dto.setIsDangerousGoods(mainCarriageTransportMovement.isDangerousGoodsIndicator()));
        }
        if (consignmentEntity.getUsedTransportEquipments() != null) {
            consignmentEntity.getUsedTransportEquipments().forEach(usedTransportEquipment -> {
                TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
                transportVehicleDto.setVehicleId(usedTransportEquipment.getEquipmentId());
                transportVehicleDto.setVehicleCountry(usedTransportEquipment.getRegistrationCountry());
                dto.getTransportVehicles().add(transportVehicleDto);
            });
        }
        return dto;
    }

    public List<IdentifiersDto> entityListToDtoList(final List<Consignment> consignmentEntity) {
        return consignmentEntity.stream().map(this::entityToDto).toList();
    }

    private OffsetDateTime fromDateTime(DateTime dateTime) {
        return switch (dateTime.getFormatId()) {
            case "102" -> {
                LocalDate localDate = LocalDate.parse(dateTime.getValue(), DateTimeFormatter.ofPattern("yyyyMMdd"));
                yield localDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            case "205" -> OffsetDateTime.parse(dateTime.getValue(), DateTimeFormatter.ofPattern("yyyyMMddHHmmZ"));
            default -> throw new RuntimeException("Unsupported formatId: " + dateTime.getFormatId());
        };
    }

    public Consignment dtoToEntity(SaveIdentifiersRequest request) {
        Consignment consignment = new Consignment();
        consignment.setDatasetId(request.getDatasetId());
        SupplyChainConsignment sourceConsignment = request.getConsignment();

        consignment.setCarrierAcceptanceDatetime(fromDateTime(sourceConsignment.getCarrierAcceptanceDateTime()));
        consignment.setDeliveryEventActualOccurrenceDatetime(fromDateTime(sourceConsignment.getDeliveryEvent().getActualOccurrenceDateTime()));

        consignment.getMainCarriageTransportMovements().addAll(sourceConsignment.getMainCarriageTransportMovement().stream().map(movement -> {
            MainCarriageTransportMovement mainCarriageTransportMovement = new MainCarriageTransportMovement();
            mainCarriageTransportMovement.setDangerousGoodsIndicator(movement.isDangerousGoodsIndicator());
            mainCarriageTransportMovement.setModeCode(Short.parseShort(movement.getModeCode()));
            mainCarriageTransportMovement.setUsedTransportMeansId(movement.getUsedTransportMeans().getId().getValue());
            mainCarriageTransportMovement.setUsedTransportMeansRegistrationCountry(movement.getUsedTransportMeans().getRegistrationCountry().getCode().value());
            mainCarriageTransportMovement.setConsignment(consignment);
            return mainCarriageTransportMovement;
        }).toList());

        consignment.setUsedTransportEquipments(sourceConsignment.getUsedTransportEquipment().stream().map(equipment -> {
            UsedTransportEquipment usedTransportEquipment = new UsedTransportEquipment();
            usedTransportEquipment.setEquipmentId(equipment.getId().getValue());
            usedTransportEquipment.setIdSchemeAgencyId(equipment.getId().getSchemeAgencyId());
            usedTransportEquipment.setRegistrationCountry(equipment.getRegistrationCountry().getCode().value());
            usedTransportEquipment.setSequenceNumber(equipment.getSequenceNumber().intValue());

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
}
