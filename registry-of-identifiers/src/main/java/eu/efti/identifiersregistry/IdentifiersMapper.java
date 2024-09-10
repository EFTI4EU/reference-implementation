package eu.efti.identifiersregistry;

import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.identifiersregistry.entity.Identifiers;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IdentifiersMapper {

    private final ModelMapper mapper;

    public Identifiers dtoToEntity(final IdentifiersDto identifiersDto) {
        return mapper.map(identifiersDto, Identifiers.class);
    }

    public IdentifiersDto entityToDto(final Identifiers identifiersEntity) {
        return mapper.map(identifiersEntity, IdentifiersDto.class);
    }

    public List<IdentifiersDto> entityListToDtoList(final List<Identifiers> identifiersEntity) {
        return Arrays.asList(mapper.map(identifiersEntity, IdentifiersDto[].class));
    }
}
