package eu.efti.commons.dto.identifiers.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentifierRequestResultDto {

    private String gateIndicator;
    private String status;
    private String errorCode;
    private String errorDescription;
    private List<ConsignmentApiDto> consignments;
}
