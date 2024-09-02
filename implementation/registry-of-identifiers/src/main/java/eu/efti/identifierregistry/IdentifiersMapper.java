package eu.efti.identifierregistry;

import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.identifierregistry.entity.Consignment;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IdentifiersMapper {

    private final ModelMapper mapper;

    public Consignment dtoToEntity(final ConsignmentIdentifiersDTO consignmentIdentifiersDTO) {
        return mapper.map(consignmentIdentifiersDTO, Consignment.class);
    }

    public ConsignmentIdentifiersDTO entityToDto(final Consignment consignment) {
        return mapper.map(consignment, ConsignmentIdentifiersDTO.class);
    }

    public List<ConsignmentIdentifiersDTO> entityListToDtoList(final List<Consignment> consignment) {
        return Arrays.asList(mapper.map(consignment, ConsignmentIdentifiersDTO[].class));
    }
}
