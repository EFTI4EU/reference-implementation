package eu.efti.metadataregistry;

import eu.efti.commons.dto.MetadataDto;
import eu.efti.metadataregistry.entity.MetadataEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MetadataMapper {

    private final ModelMapper mapper;

    public MetadataEntity dtoToEntity(final MetadataDto metadataDto) {
        return mapper.map(metadataDto, MetadataEntity.class);
    }

    public MetadataDto entityToDto(final MetadataEntity metadataEntity) {
        return mapper.map(metadataEntity, MetadataDto.class);
    }

    public List<MetadataDto> entityListToDtoList(final List<MetadataEntity> metadataEntity) {
        return Arrays.asList(mapper.map(metadataEntity, MetadataDto[].class));
    }
}
