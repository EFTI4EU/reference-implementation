package com.ingroupe.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.commons.enums.StatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static com.ingroupe.efti.commons.enums.ErrorCodesEnum.UUID_NOT_FOUND;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
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
    private SearchParameter transportMetaData;
    private String fromGateUrl;
    private AuthorityDto authority;
    private ErrorDto error;
    private MetadataResultsDto metadataResults;
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
        return !(isError() && UUID_NOT_FOUND.name().equals(this.getError().getErrorCode()));
    }
}
