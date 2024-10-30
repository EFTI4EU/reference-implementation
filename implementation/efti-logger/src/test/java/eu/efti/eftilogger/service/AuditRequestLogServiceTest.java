package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.AuthorityDto;
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

import static eu.efti.eftilogger.model.ComponentType.GATE;

@ExtendWith(MockitoExtension.class)
class AuditRequestLogServiceTest extends AbstractTestService {

    private AuditRequestLogService auditRequestLogService;

    private ControlDto controlDto;
    private MessagePartiesDto messagePartiesDto;
    private final StatusEnum status = StatusEnum.COMPLETE;
    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    public void init() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(LogService.class)).addAppender(logWatcher);

        controlDto = ControlDto.builder()
                .id(1)
                .authority(AuthorityDto.builder()
                        .name("name")
                        .nationalUniqueIdentifier("nui").build())
                .requestType(RequestTypeEnum.EXTERNAL_UIL_SEARCH)
                .requestId("requestId")
                .subsetEuRequested("subsetEu")
                .subsetMsRequested("subsetMS")
                .eftiDataUuid("dataUuid")
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
        final String expected = "{\"messageDate\":\"2024-07-31 15:05:53\",\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"sender\",\"requestingComponentCountry\":\"senderCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"receiver\",\"respondingComponentCountry\":\"receiverCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"DEFAULT_ERROR\",\"errorDescriptionMessage\":\"Error\",\"timeoutComponentType\":\"timeoutComponentType\",\"requestId\":\"requestId\",\"responseId\":\"responseId\",\"authorityNationalUniqueIdentifier\":\"nui\",\"authorityName\":\"name\",\"officerId\":\"officerId\",\"subsetEURequested\":\"subsetEu\",\"subsetMSRequested\":\"subsetMS\",\"requestType\":\"UIL_ACK\",\"eFTIDataId\":\"dataUuid\"}";
        auditRequestLogService.log(controlDto, messagePartiesDto, gateId, gateCountry, body, status, true, "name");
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(),
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("messageDate", (o1, o2) -> true)));
    }

    @Test
    void shouldLogAckFalse() throws JSONException {
        final String expected = "{\"messageDate\":\"2024-07-31 15:05:53\",\"componentType\":\"GATE\",\"componentId\":\"gateId\",\"componentCountry\":\"gateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"sender\",\"requestingComponentCountry\":\"senderCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"receiver\",\"respondingComponentCountry\":\"receiverCountry\",\"messageContent\":\"body\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"DEFAULT_ERROR\",\"errorDescriptionMessage\":\"Error\",\"timeoutComponentType\":\"timeoutComponentType\",\"requestId\":\"requestId\",\"responseId\":\"responseId\",\"authorityNationalUniqueIdentifier\":\"nui\",\"authorityName\":\"name\",\"officerId\":\"officerId\",\"subsetEURequested\":\"subsetEu\",\"subsetMSRequested\":\"subsetMS\",\"requestType\":\"UIL\",\"eFTIDataId\":\"dataUuid\"}";
        auditRequestLogService.log(controlDto, messagePartiesDto, gateId, gateCountry, body, status, false, "name");
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(),
                new CustomComparator(JSONCompareMode.LENIENT,
                        new Customization("messageDate", (o1, o2) -> true)));
    }
}
