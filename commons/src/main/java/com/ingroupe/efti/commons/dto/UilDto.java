package com.ingroupe.efti.commons.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class UilDto extends AbstractUilDto implements ValidableDto {

    @Valid
    @NotNull(message= "AUTHORITY_MISSING")
    @Schema( example = "see AuthorityDto")
    private AuthorityDto authority;
}
