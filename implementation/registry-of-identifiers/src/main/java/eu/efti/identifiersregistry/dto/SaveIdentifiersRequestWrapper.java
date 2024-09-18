package eu.efti.identifiersregistry.dto;

import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaveIdentifiersRequestWrapper {
    private String platformId;
    private SaveIdentifiersRequest saveIdentifiersRequest;
}
