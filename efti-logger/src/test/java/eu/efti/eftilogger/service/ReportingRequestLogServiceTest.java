package eu.efti.eftilogger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ErrorDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftilogger.model.RequestTypeLog;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static eu.efti.eftilogger.model.ComponentType.GATE;

@ExtendWith(MockitoExtension.class)
class ReportingRequestLogServiceTest extends AbstractTestService {

    private ReportingRequestLogService reportingRequestLogService;

    private ControlDto controlDto;
    private RequestDto requestDto;
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

        controlDto = ControlDto.builder()
                .id(1)
                .requestType(RequestTypeEnum.EXTERNAL_UIL_SEARCH)
                .requestId("requestId")
                .subsetIds(List.of("full"))
                .datasetId("dataUuid")
                .error(ErrorDto.fromErrorCode(ErrorCodesEnum.DEFAULT_ERROR))
                .status(StatusEnum.COMPLETE)
                .build();

        reportingRequestLogService = new ReportingRequestLogService(serializeUtils);

        requestDto = RequestDto.builder()
                .status(RequestStatusEnum.SUCCESS)
                .requestType(RequestType.NOTE)
                .control(controlDto)
                .gateIdDest("borduria")
                .edeliveryMessageId("edeliveryMessageId")
                .createdDate(OffsetDateTime.now())
                .id(81)
                .sentDate(OffsetDateTime.now())
                .build();
    }

    @Test
    void logReportingRequestSendDateTrueTest() throws JSONException {
        final String expected = "{\"messageDate\":\"2025-04-30 12:21:18.747\",\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"requestComponentId\",\"requestingComponentCountry\":\"requestComponentCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"respondingComponentId\",\"respondingComponentCountry\":\"respondingComponentCountry\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"DEFAULT_ERROR\",\"errorDescriptionMessage\":\"Error\",\"sentDate\":\"2025-04-30 14:21:18.746\",\"responseDelay\":0,\"requestId\":\"requestId\",\"requestType\":\"NOTE\"}";
        reportingRequestLogService.logReportingRequest(controlDto, requestDto, "currentGateId", "currentGateCountry", RequestTypeLog.NOTE, GATE, "requestComponentId", "requestComponentCountry", GATE, "respondingComponentId", "respondingComponentCountry", true);
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(), messageDateComparator);
    }

    @Test
    void logReportingRequestSendDateFalseTest() throws JSONException {
        final String expected = "{\"messageDate\":\"2025-04-30 12:21:18.737\",\"componentType\":\"GATE\",\"componentId\":\"currentGateId\",\"componentCountry\":\"currentGateCountry\",\"requestingComponentType\":\"GATE\",\"requestingComponentId\":\"requestComponentId\",\"requestingComponentCountry\":\"requestComponentCountry\",\"respondingComponentType\":\"GATE\",\"respondingComponentId\":\"respondingComponentId\",\"respondingComponentCountry\":\"respondingComponentCountry\",\"statusMessage\":\"COMPLETE\",\"errorCodeMessage\":\"DEFAULT_ERROR\",\"errorDescriptionMessage\":\"Error\",\"sentDate\":\"2025-04-30T14:21:18.735929600Z\",\"responseDelay\":1,\"requestId\":\"requestId\",\"requestType\":\"NOTE\"}";
        reportingRequestLogService.logReportingRequest(controlDto, requestDto, "currentGateId", "currentGateCountry", RequestTypeLog.NOTE, GATE, "requestComponentId", "requestComponentCountry", GATE, "respondingComponentId", "respondingComponentCountry", false);
        JSONAssert.assertEquals(expected, logWatcher.list.get(0).getFormattedMessage(), messageDateComparator);
    }
}
