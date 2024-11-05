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
public class NotesRequestDto extends RequestDto {
    private String note;
    private String platformId;

    public NotesRequestDto(final ControlDto controlDto) {
        super(controlDto);
        this.setRequestType(RequestType.NOTE);
        this.setNote(controlDto.getNotes());
        this.setPlatformId(controlDto.getPlatformId());
    }
}
