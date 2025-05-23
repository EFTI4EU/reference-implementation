package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.LoggerFactory;

import static eu.efti.eftilogger.model.ComponentType.GATE;

@ExtendWith(MockitoExtension.class)
class ReportingRegistryLogServiceTest extends AbstractTestService {

    private ReportingRegistryLogService reportingRegistryLogService;
    private ListAppender<ILoggingEvent> logWatcher;
    private CustomComparator messageDateComparator;

    @BeforeEach
    void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        messageDateComparator = new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("messageDate", (o1, o2) -> true),
                new Customization("sentDate", (o1, o2) -> true),
                new Customization("responseDelay", (o1, o2) -> true)
        );


        reportingRegistryLogService = new ReportingRegistryLogService(serializeUtils);

    }

    @Test
    void logRegistryRequestSendDateTest() throws JSONException {
        final String expected = "{\"messageDate\":\"2025-04-30 12:21:18.763\",\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"respondingComponentId\",\"requestingComponentCountry\":\"respondingComponentCountry\",\"statusMessage\":\"COMPLETE\",\"sentDate\":\"2025-04-30 12:21:18.763\",\"interfaceType\":\"EDELIVERY\"}";
        reportingRegistryLogService.logRegistryRequest("currentGateId", "currentGateCountry", GATE, "respondingComponentId", "respondingComponentCountry", new SaveIdentifiersRequestWrapper("platformId", new SaveIdentifiersRequest()), null);
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(), messageDateComparator);
    }
}
