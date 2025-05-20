package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.dto.identifiers.api.ConsignmentApiDto;
import eu.efti.commons.dto.identifiers.api.IdentifierRequestResultDto;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import eu.efti.eftilogger.service.ReportingRequestLogService;
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
    @Mock
    private ReportingRequestLogService reportingRequestLogService;

    private ControlDto controlDto;
    private UilDto uilDto;
    private static final String BODY = "body";
    private static final String RECEIVER = "receiver";

    @BeforeEach
    void setUp() {
        gateProperties = GateProperties.builder().owner("ownerId").country("ownerCountry").build();
        logManager = new LogManager(gateProperties, eftiGateIdResolver, auditRequestLogService, auditRegistryLogService, reportingRequestLogService, serializeUtils);
        controlDto = ControlDto.builder()
                .requestType(RequestTypeEnum.LOCAL_UIL_SEARCH)
                .platformId("platformId")
                .id(1).build();
        uilDto = UilDto.builder()
                .gateId("gateId").build();
    }

    @Test
    void logReceivedNoteTest() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("receiver")
                .respondingComponentType(GATE)
                .requestType("NOTE")
                .respondingComponentCountry("ownerCountry").build();

        logManager.logReceivedNote(controlDto, BODY, RECEIVER, GATE, GATE, true, "test");

        final String bodyBase64 = serializeUtils.mapObjectToBase64String(BODY);
        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", bodyBase64, StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void logSentMessageErrorTest() {
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
                .requestingComponentType(null)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        requestDto.setRequestType(RequestType.IDENTIFIER);

        logManager.logAckMessage(controlDto, GATE, true, requestDto, "test");

        verify(auditRequestLogService).logAck(controlDto, expectedMessageParties, "ownerId", "ownerCountry", "", StatusEnum.COMPLETE, RequestType.IDENTIFIER, "test");
    }

    @Test
    void testLogAckMessageError() {
        final MessagePartiesDto expectedMessageParties = MessagePartiesDto.builder()
                .requestingComponentType(null)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        requestDto.setRequestType(RequestType.UIL);

        logManager.logAckMessage(controlDto, GATE, false, requestDto, "test");

        verify(auditRequestLogService).logAck(controlDto, expectedMessageParties, "ownerId", "ownerCountry", "", StatusEnum.ERROR, RequestType.UIL, "test");
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
        final IdentifiersResponseDto identifiersResponseDto = IdentifiersResponseDto.builder()
                .identifiers(List.of(IdentifierRequestResultDto.builder()
                        .consignments(consignmentDtos).build())).build();
        final String body = serializeUtils.mapObjectToBase64String(identifiersResponseDto);

        logManager.logLocalIdentifierMessage(controlDto, identifiersResponseDto, GATE, GATE, "test");

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
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(REGISTRY)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(GATE)
                .respondingComponentCountry("ownerCountry").build();
        verify(auditRequestLogService).log(controlDto, messagePartiesDto, "ownerId", "ownerCountry", "body", StatusEnum.COMPLETE, false, "test");
    }

    @Test
    void logRegistryMetadataTest() {
        final List<ConsignmentDto> consignmentDtoList = List.of(ConsignmentDto.builder().build());
        final String body = serializeUtils.mapObjectToBase64String(consignmentDtoList);

        logManager.logRegistryIdentifiers(controlDto, consignmentDtoList, GATE, REGISTRY, "test");

        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentId("ownerId")
                .requestingComponentType(GATE)
                .requestingComponentCountry("ownerCountry")
                .respondingComponentId("ownerId")
                .respondingComponentType(REGISTRY)
                .respondingComponentCountry("ownerCountry").build();
        verify(auditRequestLogService).log(controlDto, messagePartiesDto, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
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

        final IdentifierRequestResultDto identifierRequestResultDto = IdentifierRequestResultDto.builder()
                .consignments(consignmentDtos).build();

        final IdentifiersResponseDto identifiersResponseDto = IdentifiersResponseDto.builder().identifiers(List.of(identifierRequestResultDto)).build();
        final String body = serializeUtils.mapObjectToBase64String(identifiersResponseDto);

        logManager.logFromIdentifier(identifiersResponseDto, GATE, GATE, controlDto, "test");

        verify(auditRequestLogService).log(controlDto, expectedMessageParties, "ownerId", "ownerCountry", body, StatusEnum.COMPLETE, false, "test");
    }


}
