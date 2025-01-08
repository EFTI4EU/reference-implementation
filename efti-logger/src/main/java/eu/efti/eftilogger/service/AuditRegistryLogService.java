package eu.efti.eftilogger.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftilogger.LogMarkerEnum;
import eu.efti.eftilogger.dto.LogRegistryDto;
import eu.efti.eftilogger.model.ComponentType;
import lombok.RequiredArgsConstructor;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AuditRegistryLogService implements LogService<LogRegistryDto> {

    private static final LogMarkerEnum MARKER = LogMarkerEnum.REGISTRY;
    private static final String EDELIVERY = "EDELIVERY";
    private final SerializeUtils serializeUtils;

    public void logByControlDto(final ControlDto controlDto,
                                final String currentGateId,
                                final String currentGateCountry,
                                final ComponentType requestedComponentType,
                                final ComponentType respondingComponentType,
                                final String body,
                                final String errorCode,
                                final String name) {
        final boolean isError = errorCode != null;
        this.log(LogRegistryDto.builder()
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .name(name)
                .componentType(ComponentType.GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestingComponentType(requestedComponentType)
                .requestingComponentId(currentGateId)
                .requestingComponentCountry(currentGateCountry)
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(currentGateId)
                .respondingComponentCountry(currentGateCountry)
                .messageContent(body)
                .statusMessage(isError ? StatusEnum.ERROR.name() : StatusEnum.COMPLETE.name())
                .errorCodeMessage(isError ? errorCode : "")
                .errorDescriptionMessage(isError ? ErrorCodesEnum.valueOf(errorCode).getMessage() : "")
                .eFTIDataId(controlDto.getDatasetId())
                .interfaceType(EDELIVERY)
                .build());
    }

    public void log(final SaveIdentifiersRequestWrapper requestWrapper,
                    final String currentGateId,
                    final String currentGateCountry,
                    final ComponentType respondingComponentType,
                    final ComponentType requestingComponentType,
                    final String requestingComponentId,
                    final String respondingComponentId,
                    final String body,
                    final String name) {
        String datasetId = requestWrapper.getSaveIdentifiersRequest().getDatasetId();
        this.log(LogRegistryDto.builder()
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .name(name)
                .componentType(ComponentType.GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(requestingComponentId)
                .requestingComponentCountry(currentGateCountry)
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(respondingComponentId)
                .respondingComponentCountry(currentGateCountry)
                .messageContent(body)
                .statusMessage(StatusEnum.COMPLETE.name())
                .errorCodeMessage("")
                .errorDescriptionMessage("")
                .identifiersId(datasetId)
                .eFTIDataId(datasetId)
                .interfaceType(EDELIVERY)
                .build());
    }

    @Override
    public void log(final LogRegistryDto data) {
        final String content = serializeUtils.mapObjectToJsonString(data);
        logger.info(MarkerFactory.getMarker(MARKER.name()), content);
    }
}
