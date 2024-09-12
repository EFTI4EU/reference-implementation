package eu.efti.identifiersregistry;

import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.TransportVehicleDto;
import eu.efti.identifiersregistry.entity.Consignment;
import eu.efti.identifiersregistry.entity.UsedTransportEquipment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IdentifiersMapper {

    public Consignment dtoToEntity(final IdentifiersDto identifiersDto) {
        Consignment consignment = new Consignment();
        consignment.setGateId(identifiersDto.getEFTIGateUrl());
        consignment.setPlatformId(identifiersDto.getEFTIPlatformUrl());
        consignment.setDatasetId(identifiersDto.getEFTIDataUuid());
        ArrayList<UsedTransportEquipment> usedTransportEquipments = new ArrayList<>();
        identifiersDto.getTransportVehicles().forEach(transportVehicle -> {
            UsedTransportEquipment usedTransportEquipment = new UsedTransportEquipment();
            usedTransportEquipment.setEquipmentId(transportVehicle.getVehicleId());
            usedTransportEquipment.setRegistrationCountry(transportVehicle.getVehicleCountry());
            usedTransportEquipments.add(usedTransportEquipment);
        });
        consignment.setUsedTransportEquipments(usedTransportEquipments);
        return consignment;
    }

    public IdentifiersDto entityToDto(final Consignment consignmentEntity) {
        IdentifiersDto dto = new IdentifiersDto();
        dto.setEFTIGateUrl(consignmentEntity.getGateId());
        dto.setEFTIPlatformUrl(consignmentEntity.getPlatformId());
        dto.setEFTIDataUuid(consignmentEntity.getDatasetId());
        consignmentEntity.getUsedTransportEquipments().forEach(usedTransportEquipment -> {
            TransportVehicleDto transportVehicleDto = new TransportVehicleDto();
            transportVehicleDto.setVehicleId(usedTransportEquipment.getEquipmentId());
            transportVehicleDto.setVehicleCountry(usedTransportEquipment.getRegistrationCountry());
            dto.getTransportVehicles().add(transportVehicleDto);
        });
        return dto;
    }

    public List<IdentifiersDto> entityListToDtoList(final List<Consignment> consignmentEntity) {
        return consignmentEntity.stream().map(this::entityToDto).toList();
    }
}
