package eu.efti.eftigate.mapper;

import eu.efti.commons.dto.MetadataDto;
import eu.efti.commons.dto.MetadataResultDto;
import eu.efti.eftigate.dto.ControlDto;
import eu.efti.eftigate.dto.ErrorDto;
import eu.efti.eftigate.dto.RequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.ErrorEntity;
import eu.efti.eftigate.entity.MetadataResult;
import eu.efti.eftigate.entity.RequestEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MapperUtils {

    private final ModelMapper modelMapper;

    public ControlEntity controlDtoToControEntity(final ControlDto controlDto) {
        final ControlEntity controlEntity = modelMapper.map(controlDto, ControlEntity.class);

        //ça marche pas sinon
        if (controlDto.getError() != null) {
            final ErrorEntity errorEntity = new ErrorEntity();
            errorEntity.setErrorCode(controlDto.getError().getErrorCode());
            errorEntity.setErrorDescription(controlDto.getError().getErrorDescription());
            errorEntity.setId(controlDto.getError().getId());
            controlEntity.setError(errorEntity);
        }
        return controlEntity;
    }

    public ErrorEntity errorDtoToErrorEntity(final ErrorDto errorDto) {
        return modelMapper.map(errorDto, ErrorEntity.class);
    }

    public ControlDto controlEntityToControlDto(final ControlEntity controlEntity) {
        return modelMapper.map(controlEntity, ControlDto.class);
    }

    public RequestEntity requestDtoToRequestEntity(final RequestDto requestDto) {
        return modelMapper.map(requestDto, RequestEntity.class);
    }

    public RequestDto requestToRequestDto(final RequestEntity requestEntity) {
        return modelMapper.map(requestEntity, RequestDto.class);
    }

    public List<MetadataResult> metadataDtosToMetadataEntities(final List<MetadataDto> metadataDtoList) {
        return CollectionUtils.emptyIfNull(metadataDtoList).stream()
                .map(metadataDto -> modelMapper.map(metadataDto, MetadataResult.class))
                .toList();
    }

    public List<MetadataResultDto> metadataResultEntitiesToMetadataResultDtos(final List<MetadataResult> metadataResultList) {
        return CollectionUtils.emptyIfNull(metadataResultList).stream()
                .map(metadataResult -> modelMapper.map(metadataResult, MetadataResultDto.class))
                .toList();
    }

    public List<MetadataResult> metadataResultDtosToMetadataEntities(final List<MetadataResultDto> metadataResultDtos) {
        return CollectionUtils.emptyIfNull(metadataResultDtos).stream()
                .map(metadataResultDto -> modelMapper.map(metadataResultDto, MetadataResult.class))
                .toList();
    }
}
