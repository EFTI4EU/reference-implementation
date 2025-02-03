package eu.efti.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UilDto extends AbstractUilDto implements ValidableDto {
    private static final String REGEX_URI = "^[-@./#&+\\w\\s]*$";

    private List<String> subsetIds = new ArrayList<>();
}
