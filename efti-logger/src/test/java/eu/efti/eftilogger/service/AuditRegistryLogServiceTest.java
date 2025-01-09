package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.ControlDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.REGISTRY;
import static org.assertj.core.api.Assertions.assertThat;

class AuditRegistryLogServiceTest extends AbstractTestService {

    private AuditRegistryLogService auditRegistryLogService;

    private ListAppender<ILoggingEvent> logWatcher;

    private ControlDto controlDto;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        auditRegistryLogService = new AuditRegistryLogService(serializeUtils);

        controlDto = ControlDto.builder()
                .datasetId("eftiDataUuid").build();
    }

    @Test
    void logByControlDto() {
        final String expected = "\"name\":\"name\",\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"currentGateId\",\"requestingComponentCountry\":\"currentGateCountry\",\"respondingComponentType\":\"REGISTRY\",\"respondingComponentId\":\"currentGateId\",\"respondingComponentCountry\":\"currentGateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"identifiersId\":null,\"eFTIDataId\":\"eftiDataUuid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"eftiDataUuid\"";
        auditRegistryLogService.logByControlDto(controlDto, "currentGateId", "currentGateCountry", GATE, REGISTRY, "body", null, "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }
}
