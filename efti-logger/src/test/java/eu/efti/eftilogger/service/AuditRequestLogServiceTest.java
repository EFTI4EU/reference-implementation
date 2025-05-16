package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftilogger.dto.MessagePartiesDto;
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

import java.util.List;

import static eu.efti.eftilogger.model.ComponentType.GATE;

@ExtendWith(MockitoExtension.class)
class AuditRequestLogServiceTest extends AbstractTestService {

    private AuditRequestLogService auditRequestLogService;

    private ControlDto controlDto;
    private MessagePartiesDto messagePartiesDto;
    private final StatusEnum status = StatusEnum.COMPLETE;
    private ListAppender<ILoggingEvent> logWatcher;
    private CustomComparator messageDateComparator;

    @BeforeEach
    void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        messageDateComparator = new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("messageDate", (o1, o2) -> true));

        controlDto = ControlDto.builder()
                .id(1)
                .requestType(RequestTypeEnum.EXTERNAL_UIL_SEARCH)
                .requestId("requestId")
                .subsetIds(List.of("full"))
                .datasetId("dataUuid")
                .error(ErrorDto.fromErrorCode(ErrorCodesEnum.DEFAULT_ERROR))
                .build();

        messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentId("sender")
                .requestingComponentType(GATE)
                .requestingComponentCountry("senderCountry")
                .respondingComponentId("receiver")
                .respondingComponentType(GATE)
                .respondingComponentCountry("receiverCountry").build();
        auditRequestLogService = new AuditRequestLogService(serializeUtils);
    }

    @Test
    void shouldLogAckTrue() throws JSONException {
        final String expected = "{\"messageDate\":\"2024-07-31 15:05:53\",\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"sender\",\"requestingComponentCountry\":\"senderCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"receiver\",\"respondingComponentCountry\":\"receiverCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"DEFAULT_ERROR\",\"errorDescriptionMessage\":\"Error\",\"requestId\":\"requestId\",\"subsetIds\":[\"full\"],\"requestType\":\"UIL_ACK\",\"eFTIDataId\":\"dataUuid\"}";
        auditRequestLogService.log(controlDto, messagePartiesDto, GATE_ID, GATE_COUNTRY, BODY, status, true, "name");
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(), messageDateComparator);
    }

    @Test
    void shouldLogAckFalse() throws JSONException {
        final String expected = "{\"messageDate\":\"2024-07-31 15:05:53\",\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"sender\",\"requestingComponentCountry\":\"senderCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"receiver\",\"respondingComponentCountry\":\"receiverCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"DEFAULT_ERROR\",\"errorDescriptionMessage\":\"Error\",\"requestId\":\"requestId\",\"subsetIds\":[\"full\"],\"requestType\":\"UIL\",\"eFTIDataId\":\"dataUuid\"}";
        auditRequestLogService.log(controlDto, messagePartiesDto, GATE_ID, GATE_COUNTRY, BODY, status, false, "name");
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(), messageDateComparator);
    }
}
