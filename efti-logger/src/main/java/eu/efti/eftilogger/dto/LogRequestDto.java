package eu.efti.eftilogger.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class LogRequestDto extends LogCommonDto {

    private String requestId;
    private List<String> subsetIds;
    private String requestType;
}
