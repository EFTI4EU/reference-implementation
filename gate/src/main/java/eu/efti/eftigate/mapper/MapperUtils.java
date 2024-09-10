package eu.efti.eftigate.mapper;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.*;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.ErrorEntity;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResult;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MapperUtils {

    private final ModelMapper modelMapper;

    public ControlEntity controlDtoToControlEntity(final ControlDto controlDto) {
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
        final ControlDto controlDto = modelMapper.map(controlEntity, ControlDto.class);
        final List<IdentifiersResult> metadataResultList = CollectionUtils.emptyIfNull(controlEntity.getRequests()).stream()
                .filter(IdentifiersRequestEntity.class::isInstance)
                .map(IdentifiersRequestEntity.class::cast)
                .filter(identifiersRequestEntity -> identifiersRequestEntity.getIdentifiersResults() != null
                        && CollectionUtils.isNotEmpty(identifiersRequestEntity.getIdentifiersResults().getIdentifiersResult()))
                .flatMap(request -> request.getIdentifiersResults().getIdentifiersResult().stream())
                .sorted(Comparator.comparing(IdentifiersResult::getEFTIGateUrl))
                .toList();
        controlDto.setIdentifiersResults(new IdentifiersResultsDto(identifierResultEntitiesToIdentifiersResultDtos(metadataResultList)));
        final byte[] byteArray = CollectionUtils.emptyIfNull(controlEntity.getRequests()).stream()
                .filter(UilRequestEntity.class::isInstance)
                .map(UilRequestEntity.class::cast)
                .map(UilRequestEntity::getReponseData)
                .filter(ArrayUtils::isNotEmpty)
                .collect(ByteArrayOutputStream::new, (byteArrayOutputStream, bytes) -> byteArrayOutputStream.write(bytes, 0, bytes.length), (arrayOutputStream, byteArrayOutputStream) -> {
                })
                .toByteArray();
        controlDto.setEftiData(byteArray);
        return controlDto;
    }

    public <T extends RequestEntity> T requestDtoToRequestEntity(final RequestDto requestDto, final Class<T> destinationClass) {
        return modelMapper.map(requestDto, destinationClass);
    }

    public <T extends RequestDto> T rabbitRequestDtoToRequestDto(final RabbitRequestDto rabbitRequestDto, final Class<T> destinationClass) {
        return modelMapper.map(rabbitRequestDto, destinationClass);
    }

    public <T extends RequestEntity, D extends RequestDto> D requestToRequestDto(final T requestEntity, final Class<D> destinationClass) {
        return modelMapper.map(requestEntity, destinationClass);
    }

    public List<IdentifiersResult> identifierDtosToIdentifierEntities(final List<IdentifiersDto> identifiersDtoList) {
        return CollectionUtils.emptyIfNull(identifiersDtoList).stream()
                .map(identifiersDto -> modelMapper.map(identifiersDto, IdentifiersResult.class))
                .toList();
    }

    public IdentifiersDto identifiersResultDtoToIdentifiersDto(final IdentifiersResultDto identifiersResultDto) {
        return modelMapper.map(identifiersResultDto, IdentifiersDto.class);
    }

    public List<IdentifiersResultDto> identifierDtosToIdentifiersResultDto(final List<IdentifiersDto> identifiersDtoList) {
        return CollectionUtils.emptyIfNull(identifiersDtoList).stream()
                .map(identifiersDto -> modelMapper.map(identifiersDto, IdentifiersResultDto.class))
                .toList();
    }

    public List<IdentifiersResultDto> identifierResultEntitiesToIdentifiersResultDtos(final List<IdentifiersResult> identifiersResultList) {
        return CollectionUtils.emptyIfNull(identifiersResultList).stream()
                .map(identifiersResult -> modelMapper.map(identifiersResult, IdentifiersResultDto.class))
                .toList();
    }

    public List<IdentifiersResult> identifierResultDtosToIdentifierEntities(final List<IdentifiersResultDto> identifiersResultDtos) {
        return CollectionUtils.emptyIfNull(identifiersResultDtos).stream()
                .map(identifiersResultDto -> modelMapper.map(identifiersResultDto, IdentifiersResult.class))
                .toList();
    }
}
