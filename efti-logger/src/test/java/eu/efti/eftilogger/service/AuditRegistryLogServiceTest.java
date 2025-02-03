package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.ControlDto;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

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
}
