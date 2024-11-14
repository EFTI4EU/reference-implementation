package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRegistryLogServiceTest extends AbstractTestService {

    private AuditRegistryLogService auditRegistryLogService;

    private ConsignmentDto consignmentDto;
    private ListAppender<ILoggingEvent> logWatcher;

    private ControlDto controlDto;
    private SaveIdentifiersRequestWrapper saveIdentifiersRequestWrapper;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        consignmentDto = ConsignmentDto.builder()
                .platformId("platformId")
                .datasetId("dataUid")
                .build();
        auditRegistryLogService = new AuditRegistryLogService(serializeUtils);

        controlDto = ControlDto.builder()
                .eftiDataUuid("eftiDataUuid").build();

        saveIdentifiersRequestWrapper = new SaveIdentifiersRequestWrapper("platformId", new SaveIdentifiersRequest());
    }

    @Test
    void shouldLogCreation() {
        final String expected = "\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformId\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"identifiersId\":null,\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(consignmentDto, GATE_ID, GATE_COUNTRY, BODY, "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }

    @Test
    void shouldLogCreationError() {
        final String expected = "\"name\":\"name\",\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformId\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"identifiersId\":null,\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(consignmentDto, GATE_ID, GATE_COUNTRY, BODY, "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }

    @Test
    void logByControlDto() {
        final String expected = "\"name\":\"name\",\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":null,\"requestingComponentCountry\":\"currentGateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"currentGateId\",\"respondingComponentCountry\":\"currentGateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"identifiersId\":null,\"eFTIDataId\":\"eftiDataUuid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"eftiDataUuid\"";
        auditRegistryLogService.logByControlDto(controlDto, "currentGateId", "currentGateCountry", "body", null, "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }

    @Test
    void logSaveIdentifiersRequestWrapperTest() {
        final String expected = "\"name\":null,\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformId\",\"requestingComponentCountry\":\"currentGateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"currentGateId\",\"respondingComponentCountry\":\"currentGateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"identifiersId\":null,\"eFTIDataId\":null,\"interfaceType\":\"EDELIVERY\",\"eftidataId\":null";
        auditRegistryLogService.log(saveIdentifiersRequestWrapper, "currentGateId", "currentGateCountry", "body", "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }
}
