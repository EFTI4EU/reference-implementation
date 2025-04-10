package eu.efti.eftilogger.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.enums.RegistryType;
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
public class ReportingRegistryLogService implements LogService<LogRegistryDto> {

    private static final LogMarkerEnum MARKER = LogMarkerEnum.REPORTING_REGISTRY;

    private static final String EDELIVERY = "EDELIVERY";

    private final SerializeUtils serializeUtils;

    @Override
    public void log(final LogRegistryDto data) {
        final String content = serializeUtils.mapObjectToJsonString(data);
        logger.info(MarkerFactory.getMarker(MARKER.name()), content);
    }

    private LogRegistryDto logRegistryDtoBuilder(final String currentGateId,
                                                 final String currentGateCountry,
                                                 final ComponentType requestComponentType,
                                                 final String requestComponentId,
                                                 final String requestComponentCountry,
                                                 final SaveIdentifiersRequestWrapper saveIdentifiersRequestWrapper,
                                                 final RegistryType registryType) {

        return LogRegistryDto
                .builder()
                .messageDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .componentType(ComponentType.GATE)
                .componentId(currentGateId)
                .componentCountry(currentGateCountry)
                .requestingComponentType(requestComponentType)
                .requestingComponentId(requestComponentId)
                .requestingComponentCountry(requestComponentCountry)
                .statusMessage(StatusEnum.COMPLETE.name())
                .errorCodeMessage(null)
                .errorDescriptionMessage(null)
                .eFTIDataId(saveIdentifiersRequestWrapper.getSaveIdentifiersRequest().getDatasetId())
                .interfaceType(EDELIVERY)
                .sentDate(DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now()))
                .registryType(registryType)
                .build();
    }

    public void logRegistryRequest(final String currentGateId,
                                   final String currentGateCountry,
                                   final ComponentType requestingComponentType,
                                   final String requestingComponentId,
                                   final String requestingComponentCountry,
                                   final SaveIdentifiersRequestWrapper saveIdentifiersRequestWrapper,
                                   final RegistryType registryType) {
        final LogRegistryDto logRegistryDto = logRegistryDtoBuilder(currentGateId,
                currentGateCountry,
                requestingComponentType,
                requestingComponentId,
                requestingComponentCountry,
                saveIdentifiersRequestWrapper,
                registryType);
        this.log(logRegistryDto);
    }

}
