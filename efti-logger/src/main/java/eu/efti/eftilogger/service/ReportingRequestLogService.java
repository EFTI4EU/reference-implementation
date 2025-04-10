package eu.efti.eftilogger.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.LogMarkerEnum;
import eu.efti.eftilogger.dto.LogRequestDto;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.model.RequestTypeLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReportingRequestLogService implements LogService<LogRequestDto> {

    private static final LogMarkerEnum MARKER = LogMarkerEnum.REPORTING_REQUEST;

    private final SerializeUtils serializeUtils;

    @Override
    public void log(final LogRequestDto data) {
        final String content = serializeUtils.mapObjectToJsonString(data);
        logger.info(MarkerFactory.getMarker(MARKER.name()), content);
    }

    private LogRequestDto logRequestDtoBuilder(final ControlDto controlDto,
                                               final RequestDto requestDto,
                                               final String currentGateId,
                                               final String currentGateCountry,
                                               final RequestTypeLog requestTypeLog,
                                               final ComponentType requestComponentType,
                                               final String requestComponentId,
                                               final String requestComponentCountry,
                                               final ComponentType respondingComponentType,
                                               final String respondingComponentId,
                                               final String respondingComponentCountry,
                                               final boolean isSendDate) {

        final OffsetDateTime offsetDateTimeNow = OffsetDateTime.now();
        LocalDateTime sendDate = LocalDateTime.now();
        Long responseDelay = null;
        if (isSendDate && requestDto != null && requestDto.getSentDate() != null) {
            sendDate = requestDto.getSentDate().toLocalDateTime();
            responseDelay = offsetDateTimeNow.toInstant().toEpochMilli() - requestDto.getSentDate().toInstant().toEpochMilli();
        } else if (!isSendDate && requestDto != null) {
            responseDelay = offsetDateTimeNow.toInstant().toEpochMilli() - requestDto.getCreatedDate().toInstant(offsetDateTimeNow.getOffset()).toEpochMilli();
        }

        return LogRequestDto
                .builder()
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .componentType(ComponentType.GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestingComponentType(requestComponentType)
                .requestingComponentId(requestComponentId)
                .requestingComponentCountry(requestComponentCountry)
                .statusMessage(controlDto.getStatus().name())
                .errorCodeMessage(controlDto.getError() != null ? controlDto.getError().getErrorCode() : null)
                .errorDescriptionMessage(controlDto.getError() != null ? controlDto.getError().getErrorDescription() : null)
                .sentDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(sendDate))
                .responseDelay(responseDelay)
                .requestId(controlDto.getRequestId())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(respondingComponentId)
                .respondingComponentCountry(respondingComponentCountry)
                .requestType(requestTypeLog.name())
                .build();
    }

    public void logReportingRequest(final ControlDto controlDto,
                                    final RequestDto requestDto,
                                    final String currentGateId,
                                    final String currentGateCountry,
                                    final RequestTypeLog requestTypeLog,
                                    final ComponentType requestComponentType,
                                    final String requestComponentId,
                                    final String requestComponentCountry,
                                    final ComponentType respondingComponentType,
                                    final String respondingComponentId,
                                    final String respondingComponentCountry,
                                    final boolean isSendDate) {
        final LogRequestDto logRequestDto = logRequestDtoBuilder(controlDto,
                requestDto,
                currentGateId,
                currentGateCountry,
                requestTypeLog,
                requestComponentType,
                requestComponentId,
                requestComponentCountry,
                respondingComponentType,
                respondingComponentId,
                respondingComponentCountry,
                isSendDate);
        this.log(logRequestDto);
    }

}
