package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static eu.efti.eftilogger.model.ComponentType.CA_APP;
import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.PLATFORM;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogManagerTest extends BaseServiceTest {

    private LogManager logManager;
    @Mock
    private AuditRequestLogService auditRequestLogService;

    @Mock
    private AuditRegistryLogService auditRegistryLogService;

    @Mock
    private MapperUtils mapperUtils;

    private ControlDto controlDto;
    private UilDto uilDto;
    private static final String BODY = "body";
    private static final String RECEIVER = "receiver";

    @BeforeEach
    public void setUp() {
        gateProperties = GateProperties.builder().owner("ownerId").country("ownerCountry").build();
        logManager = new LogManager(gateProperties, eftiGateUrlResolver, auditRequestLogService, auditRegistryLogService, serializeUtils, mapperUtils);
        controlDto = ControlDto.builder()
                .requestType(RequestTypeEnum.LOCAL_UIL_SEARCH)
                .eftiPlatformUrl("platformUrl")
                .id(1).build();
        uilDto = UilDto.builder()
                .gateId("gateUrl").build();
    }

    @Test
    void testLogSentMessageError() {
        when(eftiGateUrlResolver.resolve("receiver")).thenReturn("receiverCountry");
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("receiver")
                .respondingComponentType(PLATFORM)
                .respondingComponentCountry("receiverCountry").build();

        logManager.logSentMessage(controlDto, BODY, RECEIVER, true, false, "test");

        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);
        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.ERROR, false, "test");
    }

    @Test
    void testLogSentMessageSuccess() {
        when(eftiGateUrlResolver.resolve("receiver")).thenReturn("receiverCountry");
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("receiver")
                .respondingComponentType(GATE)
                .respondingComponentCountry("receiverCountry").build();

        logManager.logSentMessage(controlDto, BODY, RECEIVER, false, true, "test");
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogAckMessageSuccess() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("platformUrl")
                .requestingComponentType(PLATFORM)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logAckMessage(controlDto, false, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", "", StatusEnum.ERROR, true, "test");
    }

    @Test
    void testLogAckMessageError() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("platformUrl")
                .requestingComponentType(PLATFORM)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logAckMessage(controlDto, true, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", "", StatusEnum.COMPLETE, true, "test");
    }

    @Test
    void testLogReceivedMessage() {
        when(eftiGateUrlResolver.resolve("sender")).thenReturn("senderCountry");
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("sender")
                .requestingComponentType(GATE)
                .requestingComponentCountry("senderCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logReceivedMessage(controlDto, BODY, "sender", "test");

        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);
        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogLocalRegistryMessage() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final List<ConsignmentDto> consignmentDtos = List.of(ConsignmentDto.builder().build());
        final String body = serializeUtils.mapObjectToBase64String(consignmentDtos);

        logManager.logLocalRegistryMessage(controlDto, consignmentDtos, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogAppRequest() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("")
                .requestingComponentType(CA_APP)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final String body = serializeUtils.mapObjectToBase64String(uilDto);

        logManager.logAppRequest(controlDto, uilDto, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogAppResponse() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("")
                .requestingComponentType(CA_APP)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final String body = serializeUtils.mapObjectToBase64String(uilDto);

        logManager.logAppRequest(controlDto, uilDto, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }


}
