package eu.efti.eftigate.utils;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SearchParameter;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.UIL;
import eu.efti.v1.edelivery.UILQuery;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.UUID;

import static eu.efti.commons.enums.StatusEnum.PENDING;

@UtilityClass
public class ControlUtils {

    public static final String SUBSET_ID = "full";

    public static ControlDto fromGateToGateQuery(final UILQuery uilQuery, final RequestTypeEnum requestTypeEnum, final NotificationDto notificationDto, final String gateId) {
        final ControlDto controlDto = new ControlDto();
        UIL uil = uilQuery.getUil();
        controlDto.setDatasetId(uil.getDatasetId());
        controlDto.setGateId(gateId);
        controlDto.setFromGateId(notificationDto.getContent().getFromPartyId());
        controlDto.setPlatformId(uil.getPlatformId());
        controlDto.setRequestId(uilQuery.getRequestId());
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(StatusEnum.PENDING);
        controlDto.setSubsetIds(!uilQuery.getSubsetId().isEmpty() ? uilQuery.getSubsetId() : List.of(SUBSET_ID));
        return controlDto;
    }

    public static ControlDto fromUilControl(final UilDto uilDto, final RequestTypeEnum requestTypeEnum) {
        final String uuidGenerator = UUID.randomUUID().toString();

        final ControlDto controlDto = new ControlDto();
        controlDto.setDatasetId(uilDto.getDatasetId());
        controlDto.setGateId(uilDto.getGateId());
        controlDto.setPlatformId(uilDto.getPlatformId());
        controlDto.setRequestId(uuidGenerator);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(StatusEnum.PENDING);
        controlDto.setSubsetIds(!uilDto.getSubsetIds().isEmpty() ? uilDto.getSubsetIds() : List.of(SUBSET_ID));
        return controlDto;
    }

    public static ControlDto fromLocalIdentifiersControl(final SearchWithIdentifiersRequestDto identifiersRequestDto, final RequestTypeEnum requestTypeEnum) {
        final ControlDto controlDto = getControlFrom(requestTypeEnum, UUID.randomUUID().toString());
        controlDto.setTransportIdentifiers(SearchParameter.builder()
                .identifier(identifiersRequestDto.getIdentifier())
                .identifierType(identifiersRequestDto.getIdentifierType())
                .modeCode(identifiersRequestDto.getModeCode())
                .registrationCountryCode(identifiersRequestDto.getRegistrationCountryCode())
                .dangerousGoodsIndicator(identifiersRequestDto.getDangerousGoodsIndicator())
                .build());
        return controlDto;
    }

    public static ControlDto fromExternalIdentifiersControl(final IdentifierQuery identifierQuery, final RequestTypeEnum requestTypeEnum, final String fromGateId, final String gateId) {
        final ControlDto controlDto = getControlFrom(requestTypeEnum, identifierQuery.getRequestId());
        controlDto.setGateId(gateId);
        controlDto.setFromGateId(fromGateId);
        controlDto.setTransportIdentifiers(SearchParameter.builder()
                .identifier(identifierQuery.getIdentifier().getValue())
                .modeCode(identifierQuery.getModeCode())
                .registrationCountryCode(identifierQuery.getRegistrationCountryCode())
                .dangerousGoodsIndicator(identifierQuery.isDangerousGoodsIndicator())
                .build());
        return controlDto;
    }

    private static ControlDto getControlFrom(final RequestTypeEnum requestTypeEnum, final String requestId) {
        final ControlDto controlDto = new ControlDto();
        controlDto.setRequestId(requestId);
        controlDto.setRequestType(requestTypeEnum);
        controlDto.setStatus(PENDING);
        return controlDto;
    }
}
