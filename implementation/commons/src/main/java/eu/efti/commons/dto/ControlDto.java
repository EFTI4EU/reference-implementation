package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.enums.ErrorCodesEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlDto {
    private int id;
    private String eftiDataUuid;
    private String requestUuid;
    private RequestTypeEnum requestType;
    private StatusEnum status;
    private String eftiPlatformUrl;
    private String eftiGateUrl;
    private String subsetEuRequested;
    private String subsetMsRequested;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private byte[] eftiData;
    private SearchParameter transportIdentifiers;
    private String fromGateUrl;
    @ToString.Exclude
    @JsonIgnore
    private List<RequestDto> requests;
    private AuthorityDto authority;
    private ErrorDto error;
    private IdentifiersResultsDto identifierResults;
    private String notes;

    public boolean isError() {
        return StatusEnum.ERROR == status;
    }

    @JsonIgnore
    public boolean isExternalAsk() {
        return this.getRequestType() != null && this.getRequestType().isExternalAsk();
    }

    @JsonIgnore
    public boolean isFound() {
        return !(isError() && ErrorCodesEnum.UUID_NOT_FOUND.name().equals(this.getError().getErrorCode()));
    }
}
