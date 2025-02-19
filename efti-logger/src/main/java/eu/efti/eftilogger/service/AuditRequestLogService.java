package eu.efti.eftilogger.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.LogMarkerEnum;
import eu.efti.eftilogger.dto.LogRequestDto;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.model.RequestTypeLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static eu.efti.commons.constant.EftiGateConstants.IDENTIFIERS_TYPES;
import static eu.efti.commons.constant.EftiGateConstants.NOTES_TYPES;
import static eu.efti.commons.constant.EftiGateConstants.UIL_TYPES;
import static eu.efti.eftilogger.model.ComponentType.GATE;

@Service
@RequiredArgsConstructor
public class AuditRequestLogService implements LogService<LogRequestDto> {

    private static final LogMarkerEnum MARKER = LogMarkerEnum.REQUEST;
    private static final String ACK = "_ACK";

    private final SerializeUtils serializeUtils;

    public void log(final ControlDto control,
                    final MessagePartiesDto messagePartiesDto,
                    final String currentGateId,
                    final String currentGateCountry,
                    final String body,
                    final StatusEnum status,
                    final boolean isAck,
                    final String name) {
        final LogRequestDto logRequestDto = getLogRequestDto(control, messagePartiesDto, currentGateId, currentGateCountry, body, status, isAck, name);
        this.log(logRequestDto);
    }

    private LogRequestDto getLogRequestDto(final ControlDto control, final MessagePartiesDto messagePartiesDto, final String currentGateId, final String currentGateCountry, final String body, final StatusEnum status, final boolean isAck, final String name) {
        return LogRequestDto.builder()
                .name(name)
                .requestingComponentType(messagePartiesDto.getRequestingComponentType())
                .requestingComponentId(messagePartiesDto.getRequestingComponentId())
                .requestingComponentCountry(messagePartiesDto.getRequestingComponentCountry())
                .respondingComponentType(messagePartiesDto.getRespondingComponentType())
                .respondingComponentId(messagePartiesDto.getRespondingComponentId())
                .respondingComponentCountry(messagePartiesDto.getRespondingComponentCountry())
                .requestId(control.getRequestId())
                .subsetIds(control.getSubsetIds())
                .eftidataId(control.getDatasetId())
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .messageContent(body)
                .statusMessage(status.name())
                .componentType(GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestType(StringUtils.isNotBlank(messagePartiesDto.getRequestType()) ? messagePartiesDto.getRequestType() : getRequestTypeFromControl(control, isAck))
                .errorCodeMessage(control.getError() != null ? control.getError().getErrorCode() : null)
                .errorDescriptionMessage(control.getError() != null ? control.getError().getErrorDescription() : null)
                .build();
    }

    private String getRequestTypeFromControl(final ControlDto control, final boolean isAck) {
        if (control.getRequestType() == null) return "";
        if (UIL_TYPES.contains(control.getRequestType())) {
            return isAck ? RequestTypeLog.UIL_ACK.name() : RequestTypeLog.UIL.name();
        } else if (IDENTIFIERS_TYPES.contains(control.getRequestType())) {
            return isAck ? RequestTypeLog.IDENTIFIERS_ACK.name() : RequestTypeLog.IDENTIFIERS.name();
        } else if (NOTES_TYPES.contains(control.getRequestType())) {
            return isAck ? RequestTypeLog.NOTE_ACK.name() : RequestTypeLog.NOTE.name();
        }
        return "";
    }

    @Override
    public void log(final LogRequestDto data) {
        final String content = serializeUtils.mapObjectToJsonString(data);
        logger.info(MarkerFactory.getMarker(MARKER.name()), content);
    }

    public void logAck(final ControlDto control,
                       final MessagePartiesDto messagePartiesDto,
                       final String currentGateId,
                       final String currentGateCountry,
                       final String body,
                       final StatusEnum status,
                       final RequestType requestType,
                       final String name) {
        LogRequestDto logRequestDto = getLogRequestDto(control, messagePartiesDto, currentGateId, currentGateCountry, body, status, true, name);
        logRequestDto.setRequestType(requestType.name().concat(ACK));
        this.log(logRequestDto);
    }
}
