package com.ingroupe.efti.commons.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotesDto extends AbstractUilDto implements ValidableDto {
    private String requestUuid;
    @Size(max = 255, message = "NOTE_TOO_LONG")
    private String note;
}
