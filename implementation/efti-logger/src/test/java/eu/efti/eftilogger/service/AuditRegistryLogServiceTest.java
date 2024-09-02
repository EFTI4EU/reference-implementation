package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRegistryLogServiceTest extends AbstractTestService {

    private AuditRegistryLogService auditRegistryLogService;

    private ConsignmentIdentifiersDTO consign;
    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        consign = ConsignmentIdentifiersDTO.builder()
                .eFTIPlatformUrl("platformUrl")
                .consignmentUUID("consignmentUid")
                .eFTIDataUuid("dataUid")
                .build();
        auditRegistryLogService = new AuditRegistryLogService(serializeUtils);
    }

    @Test
    void shouldLogCreation() {
        final String expected = "\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformUrl\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"consignmentId\":\"consignmentUid\",\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(consign, gateId, gateCountry, body);
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }

    @Test
    void shouldLogCreationError() {
        final String expected = "\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformUrl\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"ERROR\",\"errorCodeMessage\":\"VEHICLE_ID_MISSING\",\"errorDescriptionMessage\":\"VehicleId missing.\",\"timeoutComponentType\":\"timeoutComponentType\",\"consignmentId\":\"consignmentUid\",\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(consign, gateId, gateCountry, body, "VEHICLE_ID_MISSING");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }
}
