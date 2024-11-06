package eu.efti.eftigate.utils;

import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResultsDto;
import eu.efti.commons.dto.NotesDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.NotesMessageBodyDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.UILQuery;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static eu.efti.commons.enums.StatusEnum.PENDING;

@UtilityClass
public class ControlUtils {

    public static final String SUBSET_EU_REQUESTED = "SubsetEuRequested";
    public static final String SUBSET_MS_REQUESTED = "SubsetMsRequested";

    public static ControlDto fromGateToGateQuery(final UILQuery uilQuery, final RequestTypeEnum requestTypeEnum, final NotificationDto notificationDto, final String eftiGateUrl) {
        final ControlDto controlDto = new ControlDto();
        controlDto.setEftiDataUuid(uilQuery.getUil().getDatasetId());
        controlDto.setEftiGateUrl(eftiGateUrl);
        controlDto.setFromGateUrl(notificationDto.getContent().getFromPartyId());
        controlDto.setEftiPlatformUrl(uilQuery.getUil().getPlatformId());
        controlDto.setRequestId(uilQuery.getRequestId());
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(StatusEnum.PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        controlDto.setAuthority(null);
        return controlDto;
    }

    public static ControlDto fromGateToGateNoteMessageBodyDto(final NotesMessageBodyDto messageBodyDto, final RequestTypeEnum requestTypeEnum, final NotificationDto notificationDto, final String eftiGateUrl) {
        final ControlDto controlDto = initControlDto(messageBodyDto, requestTypeEnum, notificationDto, eftiGateUrl);
        controlDto.setNotes(messageBodyDto.getNote());
        return controlDto;
    }

    private static ControlDto initControlDto(final NotesMessageBodyDto messageBodyDto, final RequestTypeEnum requestTypeEnum, final NotificationDto notificationDto, final String eftiGateUrl) {
        final ControlDto controlDto = new ControlDto();
        controlDto.setEftiDataUuid(messageBodyDto.getEFTIDataUuid());
        controlDto.setEftiGateUrl(eftiGateUrl);
        controlDto.setFromGateUrl(notificationDto.getContent().getFromPartyId());
        controlDto.setEftiPlatformUrl(messageBodyDto.getEFTIPlatformUrl());
        controlDto.setRequestId(messageBodyDto.getRequestId());
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
        controlDto.setEftiDataUuid(uilDto.getDatasetId());
        controlDto.setEftiGateUrl(uilDto.getGateId());
        controlDto.setEftiPlatformUrl(uilDto.getPlatformId());
        controlDto.setRequestId(uuidGenerator);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(StatusEnum.PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        return controlDto;
    }

    public static ControlDto fromLocalIdentifiersControl(final SearchWithIdentifiersRequestDto identifiersRequestDto, final RequestTypeEnum requestTypeEnum) {
        final AuthorityDto authorityDto = identifiersRequestDto.getAuthority();

        final ControlDto controlDto = getControlFrom(requestTypeEnum, authorityDto, UUID.randomUUID().toString());
        controlDto.setTransportIdentifiers(SearchParameter.builder()
                .identifier(identifiersRequestDto.getIdentifier())
                .identifierType(identifiersRequestDto.getIdentifierType())
                .modeCode(identifiersRequestDto.getModeCode())
                .registrationCountryCode(identifiersRequestDto.getRegistrationCountryCode())
                .dangerousGoodsIndicator(identifiersRequestDto.getDangerousGoodsIndicator())
                .build());
        return controlDto;
    }

    public static ControlDto fromExternalIdentifiersControl(final IdentifierQuery identifierQuery, final RequestTypeEnum requestTypeEnum, final String fromGateUrl, final String eftiGateUrl, final IdentifiersResultsDto identifiersResultsDto) {
        final ControlDto controlDto = getControlFrom(requestTypeEnum, null, identifierQuery.getRequestId());
        controlDto.setEftiGateUrl(eftiGateUrl);
        controlDto.setFromGateUrl(fromGateUrl);
        controlDto.setTransportIdentifiers(SearchParameter.builder()
                .identifier(identifierQuery.getIdentifier().getValue())
                .modeCode(identifierQuery.getModeCode())
                .registrationCountryCode(identifierQuery.getRegistrationCountryCode())
                .dangerousGoodsIndicator(identifierQuery.isDangerousGoodsIndicator())
                .build());
        controlDto.setIdentifiersResults(identifiersResultsDto.getConsignments());
        return controlDto;
    }

    private static ControlDto getControlFrom(final RequestTypeEnum requestTypeEnum, final AuthorityDto authorityDto, final String requestId) {
        final ControlDto controlDto = new ControlDto();
        controlDto.setRequestId(requestId);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        controlDto.setAuthority(authorityDto);
        return controlDto;
    }

    public static ControlDto fromNotesControl(final NotesDto notesDto, final RequestTypeEnum requestTypeEnum) {
        final String uuidGenerator = UUID.randomUUID().toString();

        final ControlDto controlDto = new ControlDto();
        controlDto.setEftiDataUuid(notesDto.getEFTIDataUuid());
        controlDto.setEftiGateUrl(notesDto.getEFTIGateUrl());
        controlDto.setEftiPlatformUrl(notesDto.getEFTIPlatformUrl());
        controlDto.setRequestId(uuidGenerator);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(PENDING);
        controlDto.setSubsetEuRequested(SUBSET_EU_REQUESTED);
        controlDto.setSubsetMsRequested(SUBSET_MS_REQUESTED);
        return controlDto;
    }
}
