package eu.efti.eftilogger.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
public class LogRegistryDto extends LogCommonDto {

    public final String metadataId;
    public final String eFTIDataId;
    public final String interfaceType;
}
