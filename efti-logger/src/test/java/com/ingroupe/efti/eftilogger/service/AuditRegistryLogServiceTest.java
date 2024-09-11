package com.ingroupe.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ingroupe.efti.commons.dto.MetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRegistryLogServiceTest extends AbstractTestService {

    private AuditRegistryLogService auditRegistryLogService;

    private MetadataDto metadataDto;
    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        metadataDto = MetadataDto.builder()
                .eFTIPlatformUrl("platformUrl")
                .metadataUUID("metadataUid")
                .eFTIDataUuid("dataUid")
                .build();
        auditRegistryLogService = new AuditRegistryLogService(serializeUtils);
    }

    @Test
    void shouldLogCreation() {
        final String expected = "\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformUrl\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"metadataId\":\"metadataUid\",\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(metadataDto, gateId, gateCountry, body, "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }

    @Test
    void shouldLogCreationError() {
        final String expected = "\"name\":\"name\",\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"PLATFORM\",\"requestingComponentId\":\"platformUrl\",\"requestingComponentCountry\":\"gateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"gateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"timeoutComponentType\":\"timeoutComponentType\",\"metadataId\":\"metadataUid\",\"eFTIDataId\":\"dataUid\",\"interfaceType\":\"EDELIVERY\",\"eftidataId\":\"dataUid\"";
        auditRegistryLogService.log(metadataDto, gateId, gateCountry, body, "name");
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(expected);
    }
}
