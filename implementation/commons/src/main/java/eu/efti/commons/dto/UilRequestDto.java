
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
public class UilRequestDto extends RequestDto {
    private byte[] responseData;

    public UilRequestDto(final ControlDto controlDto) {
        super(controlDto);
        this.setError(controlDto.getError());
        this.setRequestType(RequestType.UIL);
    }
}
