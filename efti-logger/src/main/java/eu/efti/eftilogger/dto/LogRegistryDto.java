package eu.efti.eftilogger.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class LogRegistryDto extends LogCommonDto {

    public final String eFTIDataId;
    public final String interfaceType;
    public final String platformId;
}
