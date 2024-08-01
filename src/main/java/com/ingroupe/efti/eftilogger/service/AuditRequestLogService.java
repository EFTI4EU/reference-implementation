package com.ingroupe.efti.eftilogger.service;

import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.commons.utils.SerializeUtils;
import com.ingroupe.efti.eftilogger.LogMarkerEnum;
import com.ingroupe.efti.eftilogger.dto.LogRequestDto;
import com.ingroupe.efti.eftilogger.dto.MessagePartiesDto;
import com.ingroupe.efti.eftilogger.model.RequestTypeLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.ingroupe.efti.commons.constant.EftiGateConstants.IDENTIFIERS_TYPES;
import static com.ingroupe.efti.commons.constant.EftiGateConstants.NOTES_TYPES;
import static com.ingroupe.efti.commons.constant.EftiGateConstants.UIL_TYPES;
import static com.ingroupe.efti.eftilogger.model.ComponentType.GATE;

@Service
@RequiredArgsConstructor
public class AuditRequestLogService implements LogService<LogRequestDto> {

    private static final LogMarkerEnum MARKER = LogMarkerEnum.REQUEST;

    public static final String OFFICER_ID = "officerId";
    public static final String RESPONSE_ID = "responseId";

    private final SerializeUtils serializeUtils;

    public void log(final ControlDto control,
                     final MessagePartiesDto messagePartiesDto,
                     final String currentGateId,
                     final String currentGateCountry,
                     final String body,
                     final StatusEnum status,
                     final boolean isAck) {

        final LogRequestDto logRequestDto = LogRequestDto.builder()
                .authorityName(control.getAuthority() != null ? control.getAuthority().getName() : null)
                .authorityNationalUniqueIdentifier(control.getAuthority() != null ? control.getAuthority().getNationalUniqueIdentifier() : null)
                .requestingComponentType(messagePartiesDto.getRequestingComponentType())
                .requestingComponentId(messagePartiesDto.getRequestingComponentId())
                .requestingComponentCountry(messagePartiesDto.getRequestingComponentCountry())
                .respondingComponentType(messagePartiesDto.getRespondingComponentType())
                .respondingComponentId(messagePartiesDto.getRespondingComponentId())
                .respondingComponentCountry(messagePartiesDto.getRespondingComponentCountry())
                .requestId(control.getRequestUuid())
                .officerId(OFFICER_ID)
                .responseId(RESPONSE_ID)
                .subsetEURequested(control.getSubsetEuRequested())
                .subsetMSRequested(control.getSubsetMsRequested())
                .eftidataId(control.getEftiDataUuid())
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .messageContent(body)
                .statusMessage(status.name())
                .componentType(GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestType(getRequestTypeFromControl(control, isAck))
                .errorCodeMessage(control.getError() != null ? control.getError().getErrorCode() : null)
                .errorDescriptionMessage(control.getError() != null ? control.getError().getErrorDescription() : null)
                .timeoutComponentType(TIMEOUT_COMPONENT_TYPE).build();
        this.log(logRequestDto);


    }

    private String getRequestTypeFromControl(final ControlDto control, final boolean isAck) {
        if(control.getRequestType() == null) return "";
        if(UIL_TYPES.contains(control.getRequestType())) {
            return isAck ? RequestTypeLog.UIL_ACK.name() : RequestTypeLog.UIL.name();
        } else if (IDENTIFIERS_TYPES.contains(control.getRequestType())) {
            return isAck ? RequestTypeLog.IDENTIFIERS_ACK.name() : RequestTypeLog.IDENTIFIERS.name();
        } else if(NOTES_TYPES.contains(control.getRequestType())) {
            return isAck ? RequestTypeLog.NOTES_ACK.name() : RequestTypeLog.NOTES.name();
        }
        return "";
    }

    @Override
    public void log(final LogRequestDto data) {
        final String content = serializeUtils.mapObjectToJsonString(data);
        logger.info(MarkerFactory.getMarker(MARKER.name()), content);
    }
}
