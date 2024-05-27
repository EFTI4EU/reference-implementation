package eu.efti.eftigate.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.IdentifiersMessageBodyDto;
import eu.efti.edeliveryapconnector.dto.MessageBodyDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.entity.MetadataResults;
import eu.efti.eftigate.entity.SearchParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static eu.efti.commons.enums.StatusEnum.PENDING;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlDto 
    public static final String SUBSET_EU_REQUESTED = "SubsetEuRequested";
    public static final String SUBSET_MS_REQUESTED = "SubsetMsRequested";
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
    @ToString.Exclude
    @JsonIgnore
    private List<RequestDto> requests;
    private AuthorityDto authority;
    private ErrorDto error;
    private MetadataResults metadataResults;

    public static ControlDto fromGateToGateMessageBodyDto(final MessageBodyDto messageBodyDto, final RequestTypeEnum requestTypeEnum, final NotificationDto notificationDto, final String eftiGateUrl) {
        final ControlDto controlDto = new ControlDto();
        controlDto.setEftiDataUuid(messageBodyDto.getEFTIDataUuid());
        controlDto.setEftiGateUrl(eftiGateUrl);
        controlDto.setFromGateUrl(notificationDto.getContent().getFromPartyId());
        controlDto.setEftiPlatformUrl(messageBodyDto.getEFTIPlatformUrl());
        controlDto.setRequestUuid(messageBodyDto.getRequestUuid());
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(StatusEnum.PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        controlDto.setAuthority(null);
        return controlDto;
    }

    public static ControlDto fromUilControl(final UilDto uilDto, final RequestTypeEnum requestTypeEnum) {
        final String uuidGenerator = UUID.randomUUID().toString();

        final ControlDto controlDto = new ControlDto();
        controlDto.setEftiDataUuid(uilDto.getEFTIDataUuid());
        controlDto.setEftiGateUrl(uilDto.getEFTIGateUrl());
        controlDto.setEftiPlatformUrl(uilDto.getEFTIPlatformUrl());
        controlDto.setRequestUuid(uuidGenerator);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(StatusEnum.PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        controlDto.setAuthority(uilDto.getAuthority());
        return controlDto;
    }

    public static ControlDto fromLocalMetadataControl(final MetadataRequestDto metadataRequestDto, final RequestTypeEnum requestTypeEnum) {
        final AuthorityDto authorityDto = metadataRequestDto.getAuthority();

        final ControlDto controlDto = getControlFrom(requestTypeEnum, authorityDto, UUID.randomUUID().toString());
        controlDto.setTransportMetaData(SearchParameter.builder()
                .vehicleId(metadataRequestDto.getVehicleID())
                .transportMode(metadataRequestDto.getTransportMode())
                .vehicleCountry(metadataRequestDto.getVehicleCountry())
                .isDangerousGoods(metadataRequestDto.getIsDangerousGoods())
                .build());
        return controlDto;
    }

    public static ControlDto fromExternalMetadataControl(final IdentifiersMessageBodyDto messageBodyDto, final RequestTypeEnum requestTypeEnum, final String fromGateUrl, final String eftiGateUrl, MetadataResults metadataResults) {
        final ControlDto controlDto = getControlFrom(requestTypeEnum, null, messageBodyDto.getRequestUuid());
        //to check
        controlDto.setEftiGateUrl(eftiGateUrl);
        controlDto.setFromGateUrl(fromGateUrl);
        controlDto.setTransportMetaData(SearchParameter.builder()
                .vehicleId(messageBodyDto.getVehicleID())
                .transportMode(messageBodyDto.getTransportMode())
                .vehicleCountry(messageBodyDto.getVehicleCountry())
                .isDangerousGoods(messageBodyDto.getIsDangerousGoods())
                .build());
        controlDto.setMetadataResults(metadataResults);
        return controlDto;
    }

    private static ControlDto getControlFrom(final RequestTypeEnum requestTypeEnum, final AuthorityDto authorityDto, final String requestUuid) {
        final ControlDto controlDto = new ControlDto();
        controlDto.setRequestUuid(requestUuid);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        controlDto.setAuthority(authorityDto);
        return controlDto;
    }

    public boolean isError() {
        return StatusEnum.ERROR == status;
    }

public boolean isExternalAsk() {
        return this.getRequestType() != null && this.getRequestType().isExternalAsk();
    }
}
