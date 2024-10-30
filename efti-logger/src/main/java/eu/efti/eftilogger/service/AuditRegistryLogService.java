package eu.efti.eftilogger.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
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
    public static final String EDELIVERY = "EDELIVERY";
    private final SerializeUtils serializeUtils;

    public void logByControlDto(final ControlDto controlDto,
                                final String currentGateId,
                                final String currentGateCountry,
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
                .requestingComponentType(ComponentType.PLATFORM)
                .requestingComponentId(controlDto.getEftiPlatformUrl())
                .requestingComponentCountry(currentGateCountry)
                .respondingComponentType(ComponentType.GATE)
                .respondingComponentId(currentGateId)
                .respondingComponentCountry(currentGateCountry)
                .messageContent(body)
                .statusMessage(isError ? StatusEnum.ERROR.name() : StatusEnum.COMPLETE.name())
                .errorCodeMessage(isError ? errorCode : "")
                .errorDescriptionMessage(isError ? ErrorCodesEnum.valueOf(errorCode).getMessage() : "")
                .timeoutComponentType(TIMEOUT_COMPONENT_TYPE)
                .eFTIDataId(controlDto.getEftiDataUuid())
                .interfaceType(EDELIVERY)
                .build());
    }

    public void log(final ConsignmentDto consignmentDto,
                    final String currentGateId,
                    final String currentGateCountry,
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
                .requestingComponentType(ComponentType.PLATFORM)
                .requestingComponentId(consignmentDto.getPlatformId())
                .requestingComponentCountry(currentGateCountry)
                .respondingComponentType(ComponentType.GATE)
                .respondingComponentId(currentGateId)
                .respondingComponentCountry(currentGateCountry)
                .messageContent(body)
                .statusMessage(isError ? StatusEnum.ERROR.name() : StatusEnum.COMPLETE.name())
                .errorCodeMessage(isError ? errorCode : "")
                .errorDescriptionMessage(isError ? ErrorCodesEnum.valueOf(errorCode).getMessage() : "")
                .timeoutComponentType(TIMEOUT_COMPONENT_TYPE)
                .eFTIDataId(consignmentDto.getDatasetId())
                .interfaceType(EDELIVERY)
                .build());
    }

    public void log(final ConsignmentDto consignmentDto,
                    final String currentGateId,
                    final String currentGateCountry,
                    final String body,
                    final String name) {
        this.log(consignmentDto, currentGateId, currentGateCountry, body, null, name);

    }

    public void log(final SaveIdentifiersRequestWrapper requestWrapper,
                    final String currentGateId,
                    final String currentGateCountry,
                    final String body,
                    final String name) {
        this.log(LogRegistryDto.builder()
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .componentType(ComponentType.GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestingComponentType(ComponentType.PLATFORM)
                .requestingComponentId(requestWrapper.getPlatformId())
                .requestingComponentCountry(currentGateCountry)
                .respondingComponentType(ComponentType.GATE)
                .respondingComponentId(currentGateId)
                .respondingComponentCountry(currentGateCountry)
                .messageContent(body)
                .statusMessage(StatusEnum.COMPLETE.name())
                .errorCodeMessage("")
                .errorDescriptionMessage("")
                .timeoutComponentType(TIMEOUT_COMPONENT_TYPE)
                .identifiersId(requestWrapper.getSaveIdentifiersRequest().getDatasetId())
                .eFTIDataId(requestWrapper.getSaveIdentifiersRequest().getDatasetId())
                .interfaceType(EDELIVERY)
                .build());
    }

    @Override
    public void log(final LogRegistryDto data) {
        final String content = serializeUtils.mapObjectToJsonString(data);
        logger.info(MarkerFactory.getMarker(MARKER.name()), content);
    }
}
