package eu.efti.eftigate.entity;

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
public class IdentifiersResults implements Serializable {
    private List<ConsignmentDto> consignments;
}
