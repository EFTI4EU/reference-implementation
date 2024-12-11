package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.dto.identifiers.api.ConsignmentApiDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.REGISTRY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogManagerTest extends BaseServiceTest {

    private LogManager logManager;
    @Mock
    private AuditRequestLogService auditRequestLogService;

    @Mock
    private AuditRegistryLogService auditRegistryLogService;

    private ControlDto controlDto;
    private UilDto uilDto;
    private static final String BODY = "body";
    private static final String RECEIVER = "receiver";

    @BeforeEach
    public void setUp() {
        gateProperties = GateProperties.builder().owner("ownerId").country("ownerCountry").build();
        logManager = new LogManager(gateProperties, eftiGateIdResolver, auditRequestLogService, auditRegistryLogService, serializeUtils);
        controlDto = ControlDto.builder()
                .requestType(RequestTypeEnum.LOCAL_UIL_SEARCH)
                .platformId("platformId")
                .id(1).build();
        uilDto = UilDto.builder()
                .gateId("gateId").build();
    }

    @Test
    void testLogSentMessageError() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("receiver")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logSentMessage(controlDto, BODY, RECEIVER, GATE, GATE, false, "test");

        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);
        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.ERROR, false, "test");
    }

    @Test
    void testLogSentMessageSuccess() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("receiver")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logSentMessage(controlDto, BODY, RECEIVER, GATE, GATE, true, "test");

        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogAckMessageSuccess() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("platformId")
                .requestingComponentType(null)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(null)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logAckMessage(controlDto, null, null, false, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", "", StatusEnum.ERROR, true, "test");
    }

    @Test
    void testLogAckMessageError() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("platformId")
                .requestingComponentType(null)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(null)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logAckMessage(controlDto, null, null, true, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", "", StatusEnum.COMPLETE, true, "test");
    }

    @Test
    void testLogReceivedMessage() {
        when(eftiGateIdResolver.resolve("sender")).thenReturn("senderCountry");
        controlDto.setStatus(StatusEnum.COMPLETE);
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("sender")
                .requestingComponentType(GATE)
                .requestingComponentCountry("senderCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();

        logManager.logReceivedMessage(controlDto, GATE, GATE, BODY, "sender", StatusEnum.COMPLETE, "test");

        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);
        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogLocalIdentifierMessage() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final List<ConsignmentApiDto> consignmentDtos = List.of(ConsignmentApiDto.builder().build());
        final String body = serializeUtils.mapObjectToBase64String(consignmentDtos);

        logManager.logLocalIdentifierMessage(controlDto, consignmentDtos, GATE, GATE, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void testLogAppRequest() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId(null)
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final String body = serializeUtils.mapObjectToBase64String(uilDto);

        logManager.logAppRequest(controlDto, uilDto, GATE, GATE, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void logAppResponseTest() {
        RequestIdDto requestIdDto = RequestIdDto.builder().requestId("requestId").status(StatusEnum.COMPLETE).build();
        controlDto.setStatus(StatusEnum.COMPLETE);
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("requestingComponentId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("respondingComponentId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final String body = serializeUtils.mapObjectToBase64String(requestIdDto);

        logManager.logAppResponse(controlDto, requestIdDto, GATE, "requestingComponentId", GATE, "respondingComponentId", "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void logRequestRegistryTest() {
        logManager.logRequestRegistry(controlDto, "body", REGISTRY, GATE, "test");

        verify(auditRegistryLogService).logByControlDto(controlDto, "ownerId", "ownerCountry", REGISTRY, GATE, "body", null, "test");
    }

    @Test
    void logRegistryMetadataTest() {
        final List<ConsignmentDto> consignmentDtoList = List.of(ConsignmentDto.builder().build());
        final String body = serializeUtils.mapObjectToBase64String(consignmentDtoList);

        logManager.logRegistryIdentifiers(controlDto, consignmentDtoList, GATE, REGISTRY, "test");

        verify(auditRegistryLogService).logByControlDto(controlDto, "ownerId", "ownerCountry", GATE, REGISTRY, body, null, "test");
    }

    @Test
    void logFromMetadataTest() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        final List<ConsignmentApiDto> consignmentDtos = List.of(ConsignmentApiDto.builder().build());

        IdentifiersResponseDto identifiersResponseDto = IdentifiersResponseDto.builder().identifiers(consignmentDtos).build();
        final String body = serializeUtils.mapObjectToBase64String(consignmentDtos);

        logManager.logFromIdentifier(identifiersResponseDto, GATE, GATE, controlDto, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }


}
