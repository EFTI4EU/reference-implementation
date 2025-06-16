package eu.efti.eftigate.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.MemoryAppender;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.UilRequestEntity;
import eu.efti.eftigate.repository.ControlRepository;
import eu.efti.eftigate.repository.UilRequestRepository;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftigate.service.request.RequestServiceFactory;
import eu.efti.eftigate.service.request.UilRequestService;
import eu.efti.eftilogger.LogMarkerEnum;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import eu.efti.eftilogger.service.LogService;
import eu.efti.eftilogger.service.ReportingRequestLogService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ReportingLogTest extends AbstractServiceTest {

    @Mock
    private UilRequestRepository uilRequestRepository;
    @Mock
    private ControlService controlService;
    @Mock
    private RabbitSenderService rabbitSenderService;
    @Mock
    private GateProperties gateProperties;
    @Mock
    private RequestUpdaterService requestUpdaterService;
    @Mock
    private ValidationService validationService;
    @Mock
    private EftiGateIdResolver eftiGateIdResolver;
    @Mock
    private ControlRepository controlRepository;
    @Mock
    private RequestServiceFactory requestServiceFactory;
    @Mock
    private EftiAsyncCallsProcessor eftiAsyncCallsProcessor;
    @Mock
    private Function<List<String>, RequestTypeEnum> gateToRequestTypeFunction;
    @Mock
    private IdentifiersService identifiersService;

    private final AuditRequestLogService auditRequestLogService = new AuditRequestLogService(serializeUtils);
    private final AuditRegistryLogService auditRegistryLogService = new AuditRegistryLogService(serializeUtils);
    private final ReportingRequestLogService reportingRequestLogService = new ReportingRequestLogService(serializeUtils);

    private UilRequestService uilRequestService;
    private ControlService notMockedControlService;

    private MemoryAppender memoryAppender;
    private static final String LOGGER_NAME = LogService.class.getName();

    private final String requestId = UUID.randomUUID().toString();
    private final UilRequestEntity uilRequestEntity = new UilRequestEntity();
    private final ControlEntity controlEntity = new ControlEntity();

    @BeforeEach
    void before() {
        gateProperties = GateProperties.builder()
                .ap(GateProperties.ApConfig.builder().build())
                .country("BO")
                .owner("borduria").build();

        final LogManager logManager = new LogManager(gateProperties, eftiGateIdResolver, auditRequestLogService, auditRegistryLogService, reportingRequestLogService, serializeUtils);

        uilRequestService = new UilRequestService(uilRequestRepository, mapperUtils, rabbitSenderService, controlService,
                gateProperties, requestUpdaterService, serializeUtils, validationService, logManager, reportingRequestLogService, eftiGateIdResolver);

        notMockedControlService = new ControlService(controlRepository, eftiGateIdResolver, identifiersService, mapperUtils,
                requestServiceFactory, logManager, reportingRequestLogService, gateToRequestTypeFunction, eftiAsyncCallsProcessor,
                gateProperties, serializeUtils);
        Logger memoryAppenderTestLogger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        memoryAppender = MemoryAppender.createInitializedMemoryAppender(
                Level.TRACE, memoryAppenderTestLogger);

        controlEntity.setRequestType(RequestTypeEnum.LOCAL_UIL_SEARCH);
        controlEntity.setStatus(StatusEnum.COMPLETE);
        controlEntity.setPlatformId("acme");
        controlEntity.setRequestId(requestId);
        controlEntity.setId(1);

        uilRequestEntity.setRequestType(RequestType.UIL.name());
        uilRequestEntity.setCreatedDate(OffsetDateTime.now());
        uilRequestEntity.setControl(controlEntity);
    }

    @Test
    void localUilSearch_receiveResponse() {
        final Map<String, String> expectedLogs = Map.ofEntries(
                Map.entry("componentType", "GATE"),
                Map.entry("componentId", "borduria"),
                Map.entry("componentCountry", "BO"),
                Map.entry("requestingComponentType", "GATE"),
                Map.entry("requestingComponentId", "borduria"),
                Map.entry("requestingComponentCountry", "BO"),
                Map.entry("respondingComponentType", "PLATFORM"),
                Map.entry("respondingComponentId", "acme"),
                Map.entry("respondingComponentCountry", "BO"),
                Map.entry("statusMessage", "COMPLETE"),
                Map.entry("requestId", requestId),
                Map.entry("requestType", "UIL"));

        final String content = """
                        <uilResponse
                                xmlns="http://efti.eu/v1/edelivery"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                xsi:schemaLocation="http://efti.eu/v1/edelivery ../edelivery/gate.xsd"
                                status="200"
                                requestId="42">
                        </uilResponse>
                """;

        final NotificationDto notificationDto = NotificationDto.builder()
                .notificationType(NotificationType.RECEIVED)
                .content(NotificationContentDto.builder()
                        .body(content)
                        .contentType("text/html")
                        .fromPartyId("acme")
                        .messageId("e94806cd-e52b-11ee-b7d3-0242ac120012@domibus.eu")
                        .build())
                .build();

        when(uilRequestRepository.findByControlRequestIdAndStatus(any(), any())).thenReturn(uilRequestEntity);
        when(uilRequestRepository.save(any())).thenReturn(uilRequestEntity);
        uilRequestService.manageResponseReceived(notificationDto);

        assertTrue(memoryAppender.containsGivenFieldAndValue(expectedLogs, LogMarkerEnum.REPORTING_REQUEST.name()));
    }

    @Test
    void localUilSearch_getResult() {
        final Map<String, String> expectedLogs = Map.ofEntries(
                Map.entry("componentType", "GATE"),
                Map.entry("componentId", "borduria"),
                Map.entry("componentCountry", "BO"),
                Map.entry("requestingComponentType", "CA_APP"),
                Map.entry("requestingComponentCountry", "BO"),
                Map.entry("respondingComponentType", "GATE"),
                Map.entry("respondingComponentId", "borduria"),
                Map.entry("respondingComponentCountry", "BO"),
                Map.entry("statusMessage", "COMPLETE"),
                Map.entry("requestId", requestId),
                Map.entry("requestType", "UIL"));

        when(controlRepository.findByRequestId(any())).thenReturn(Optional.of(controlEntity));
        when(requestServiceFactory.getRequestServiceByRequestType(any(RequestTypeEnum.class))).thenReturn(uilRequestService);
        when(controlRepository.save(any())).thenReturn(controlEntity);
        when(uilRequestRepository.findByControlId(anyInt())).thenReturn(List.of(uilRequestEntity));
        notMockedControlService.getControlEntity(requestId);

        assertTrue(memoryAppender.containsGivenFieldAndValue(expectedLogs, LogMarkerEnum.REPORTING_REQUEST.name()));
    }

}
