package eu.efti.commons.dto.aap;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.ValidableDto;
import jakarta.validation.constraints.NotNull;
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
public class AapSearchWithIdentifiersRequestDto extends SearchWithIdentifiersRequestDto implements ValidableDto {
    @NotNull(message = "AUTHORITY_MISSING")
    private AuthorityDto authority;
}
