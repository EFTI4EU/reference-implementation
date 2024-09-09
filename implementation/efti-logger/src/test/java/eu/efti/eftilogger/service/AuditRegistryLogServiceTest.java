package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.IdentifiersDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRegistryLogServiceTest extends AbstractTestService {

    private AuditRegistryLogService auditRegistryLogService;

    private IdentifiersDto identifiersDto;
    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        identifiersDto = IdentifiersDto.builder()
                .eFTIPlatformUrl("platformUrl")
                .identifiersUUID("identifiersUid")
                .eFTIDataUuid("dataUid")
                .build();
        auditRegistryLogService = new AuditRegistryLogService(serializeUtils);
    }

    @Test
    void shouldLogCreation() {
        final String expected = "\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformUrl\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"identifiersId\":\"identifiersUid\",\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(identifiersDto, gateId, gateCountry, body);
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }

    @Test
    void shouldLogCreationError() {
        final String expected = "\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformUrl\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"ERROR\",\"errorCodeMessage\":\"VEHICLE_ID_MISSING\",\"errorDescriptionMessage\":\"VehicleId missing.\",\"timeoutComponentType\":\"timeoutComponentType\",\"identifiersId\":\"identifiersUid\",\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(identifiersDto, gateId, gateCountry, body, "VEHICLE_ID_MISSING");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }
}
