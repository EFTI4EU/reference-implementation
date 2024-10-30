package eu.efti.commons.dto;

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
public class SearchParameter implements Serializable {
    private String identifier;
    private List<String> identifierType;
    private Boolean dangerousGoodsIndicator;
    private String registrationCountryCode;
    private String modeCode;
}
