package eu.efti.commons.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static eu.efti.commons.enums.ErrorCodesEnum.ID_NOT_FOUND;


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
    private String datasetId;
    private String requestId;
    private RequestTypeEnum requestType;
    private StatusEnum status;
    private String platformId;
    private String gateId;
    @Builder.Default
    private List<String> subsetIds = new ArrayList<>();
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private byte[] eftiData;
    private SearchParameter transportIdentifiers;
    private String fromGateId;
    private AuthorityDto authority;
    private ErrorDto error;
    private List<ConsignmentDto> identifiersResults;
    private String notes;

    public boolean isError() {
        return StatusEnum.ERROR == status;
    }

    @JsonIgnore
    public boolean isExternalAsk() {
        return this.getRequestType() != null && this.getRequestType().isExternalAsk();
    }

    @JsonIgnore
    public boolean isLocalAsk() {
        return this.getRequestType() != null && this.getRequestType().isLocalAsk();
    }

    @JsonIgnore
    public boolean isFound() {
        return !(isError() && ID_NOT_FOUND.name().equals(this.getError().getErrorCode()));
    }
}
