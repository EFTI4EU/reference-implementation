package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentifiersResultsDto implements Serializable {
    private List<ConsignmentDto> consignments;
}
