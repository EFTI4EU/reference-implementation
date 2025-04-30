package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.LoggerFactory;

import static eu.efti.eftilogger.model.ComponentType.GATE;

class AuditRegistryLogServiceTest extends AbstractTestService {

    private AuditRegistryLogService auditRegistryLogService;

    private ListAppender<ILoggingEvent> logWatcher;

    private CustomComparator messageDateComparator;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        messageDateComparator = new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("messageDate", (o1, o2) -> true));

        auditRegistryLogService = new AuditRegistryLogService(serializeUtils);

    }

    @Test
    void shouldLogTest() throws JSONException {
        SaveIdentifiersRequest saveIdentifiersRequest = new SaveIdentifiersRequest();
        SaveIdentifiersRequestWrapper saveIdentifiersRequestWrapper = new SaveIdentifiersRequestWrapper("platformId", saveIdentifiersRequest);
        final String expected = "{\"messageDate\":\"2025-04-30 12:21:17.080\",\"name\":\"name\",\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"gateId\",\"requestingComponentCountry\":\"currentGateCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"gateId\",\"respondingComponentCountry\":\"currentGateCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"\",\"errorDescriptionMessage\":\"\",\"interfaceType\":\"EDELIVERY\"}";
        auditRegistryLogService.log(saveIdentifiersRequestWrapper,"currentGateId", "currentGateCountry", GATE, GATE,GATE_ID,GATE_ID, BODY, "name");
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(), messageDateComparator);
    }

}
