package eu.efti.commons.dto;

import eu.efti.commons.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IdentifiersRequestDto extends RequestDto {
    private MetadataResultsDto metadataResults;
    public IdentifiersRequestDto(final ControlDto controlDto) {
        super(controlDto);
        setRequestType(RequestType.IDENTIFIER);
    }
}
