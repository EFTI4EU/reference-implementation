package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.UilRequestDto;
import eu.efti.commons.dto.ValidableDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.model.RequestTypeLog;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import eu.efti.eftilogger.service.ReportingRequestLogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static eu.efti.commons.constant.EftiGateConstants.REQUEST_STATUS_ENUM_STATUS_ENUM_MAP;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.StatusEnum.COMPLETE;
import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.PLATFORM;

@Service
@RequiredArgsConstructor
public class LogManager {

    private final GateProperties gateProperties;
    private final EftiGateIdResolver eftiGateIdResolver;
    private final AuditRequestLogService auditRequestLogService;
    private final AuditRegistryLogService auditRegistryLogService;
    private final ReportingRequestLogService reportingRequestLogService;
    private final SerializeUtils serializeUtils;

    public static final String FTI_ROOT_RESPONSE_SUCESS = "send sucess to domibus";
    public static final String FTI_SEND_FAIL = "send fail to domibus";
    public static final String FTI_014 = "fti014";
    public static final String FTI_008 = "fti008";
    public static final String FTI_015 = "fti015";
    public static final String FTI_016 = "fti016";
    public static final String FTI_017 = "fti017";
    public static final String FTI_011 = "fti011";
    public static final String FTI_022 = "fti022";
    public static final String FTI_010 = "fti010";
    public static final String FTI_009 = "fti009";
    public static final String FTI_020 = "fti020";
    public static final String FTI_021 = "fti021";
    public static final String FTI_019 = "fti019";
    public static final String FTI_024 = "fti024";
    public static final String FTI_025 = "fti025";
    public static final String FTI_026 = "fti026";

    public <T> void logReceivedNote(final ControlDto control,
                                    final T message,
                                    final String receiver,
                                    final ComponentType requestingComponentType,
                                    final ComponentType respondingComponentType,
                                    final boolean isSuccess,
                                    final String name) {
        String receiverCountry = eftiGateIdResolver.resolve(receiver);
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(receiver)
                .respondingComponentCountry(StringUtils.isNotBlank(receiverCountry) ? receiverCountry : gateProperties.getCountry())
                .requestType(RequestTypeLog.NOTE.name())
                .build();
        final StatusEnum status = isSuccess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        final String body = serializeUtils.mapObjectToBase64String(message);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false, name);
    }

    private void sendLogRequest(final ControlDto control, final String message, final String receiver, final ComponentType requestingComponentType, final ComponentType respondingComponentType, final boolean isSuccess, final String name, final String receiverCountry) {
        final MessagePartiesDto messagePartiesDto = buildMessagePartiesDto(receiver, requestingComponentType, respondingComponentType, receiverCountry);
        final StatusEnum status = isSuccess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        final String body = serializeUtils.mapObjectToBase64String(message);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false, name);
    }

    private MessagePartiesDto buildMessagePartiesDto(final String receiver, final ComponentType requestingComponentType, final ComponentType respondingComponentType, final String receiverCountry) {
        return MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(receiver)
                .respondingComponentCountry(StringUtils.isNotBlank(receiverCountry) ? receiverCountry : gateProperties.getCountry())
                .build();
    }

    public void logSentMessage(final ControlDto control,
                               final String message,
                               final String receiver,
                               final ComponentType requestingComponentType,
                               final ComponentType respondingComponentType,
                               final boolean isSuccess,
                               final String name) {
        String receiverCountry = eftiGateIdResolver.resolve(receiver);
        sendLogRequest(control, message, receiver, requestingComponentType, respondingComponentType, isSuccess, name, receiverCountry);
    }

    public void logFromIdentifier(final IdentifiersResponseDto identifiersResponseDto, final ComponentType requestingComponentType, final ComponentType respondingComponentType, final ControlDto controlDto, final String name) {
        this.logLocalIdentifierMessage(controlDto, identifiersResponseDto, requestingComponentType, respondingComponentType, name);
    }

    public void logAckMessage(final ControlDto control,
                              final ComponentType respondingComponentType,
                              final boolean isSuccess,
                              final RequestDto request, final String name) {
        final boolean isLocalRequest = control.getRequestType() == RequestTypeEnum.LOCAL_UIL_SEARCH;
        final String receiver = isLocalRequest ? control.getPlatformId() : control.getGateId();
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentCountry(isLocalRequest ? gateProperties.getCountry() : eftiGateIdResolver.resolve(receiver))
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
        final StatusEnum status = isSuccess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        this.auditRequestLogService.logAck(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), "", status, request.getRequestType(), name);
    }

    public void logReceivedMessage(final ControlDto control,
                                   final ComponentType requestingComponentType,
                                   final ComponentType respondingComponentType,
                                   final String body,
                                   final String sender,
                                   final StatusEnum statusEnum,
                                   final String name) {
        final String senderCountry = eftiGateIdResolver.resolve(sender);
        final boolean senderIsKnown = senderCountry != null;
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType) // if sender is unknown, its a platform
                .requestingComponentId(sender)
                .requestingComponentCountry(senderIsKnown ? senderCountry : gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(body);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), bodyBase64, statusEnum, false, name);
    }

    public void logReceivedNoteMessage(final ControlDto control,
                                       final ComponentType requestingComponentType,
                                       final ComponentType respondingComponentType,
                                       final String body,
                                       final String sender,
                                       final StatusEnum statusEnum,
                                       final String name) {
        final String senderCountry = eftiGateIdResolver.resolve(sender);
        final boolean senderIsKnown = senderCountry != null;
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType) // if sender is unknown, its a platform
                .requestingComponentId(sender)
                .requestingComponentCountry(senderIsKnown ? senderCountry : gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry())
                .requestType(RequestTypeLog.NOTE.name())
                .build();
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(body);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), bodyBase64, statusEnum, false, name);
    }

    public void logRegistryIdentifiers(final ControlDto control,
                                       final List<ConsignmentDto> consignementList,
                                       final ComponentType requestingComponentType,
                                       final ComponentType respondingComponentType,
                                       final String name) {
        final String body = consignementList != null ? serializeUtils.mapObjectToBase64String(consignementList) : null;
        StatusEnum status = control.isError() ? StatusEnum.ERROR : StatusEnum.COMPLETE;
        final MessagePartiesDto messagePartiesDto = getMessagePartiesDto(requestingComponentType, respondingComponentType);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false, name);
    }

    public void logLocalIdentifierMessage(final ControlDto control,
                                          final IdentifiersResponseDto identifierRequestResultDtos,
                                          final ComponentType requestingComponentType, final ComponentType respondingComponentType, final String name) {
        final MessagePartiesDto messagePartiesDto = getMessagePartiesDto(requestingComponentType, respondingComponentType);
        final String body = serializeUtils.mapObjectToBase64String(identifierRequestResultDtos);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false, name);
    }

    private MessagePartiesDto getMessagePartiesDto(final ComponentType requestingComponentType, final ComponentType respondingComponentType) {
        return MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(!ComponentType.CA_APP.equals(respondingComponentType) ? gateProperties.getOwner() : StringUtils.EMPTY)
                .respondingComponentCountry(gateProperties.getCountry()).build();
    }

    public void logRequestRegistry(final ControlDto controlDto, final String body, final ComponentType requestingComponentType,
                                   final ComponentType respondingComponentType, final String name) {
        StatusEnum status = controlDto.isError() ? StatusEnum.ERROR : StatusEnum.COMPLETE;
        final MessagePartiesDto messagePartiesDto = getMessagePartiesDto(requestingComponentType, respondingComponentType);
        this.auditRequestLogService.log(controlDto, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false, name);
    }

    public <T extends ValidableDto> void logAppRequest(final ControlDto control,
                                                       final T searchDto,
                                                       final ComponentType requestingComponentType,
                                                       final ComponentType respondingComponentType,
                                                       final String name) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(control.getFromGateId())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();

        final String body = serializeUtils.mapObjectToBase64String(searchDto);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false, name);
    }

    public void logAppResponse(final ControlDto control,
                               final RequestIdDto requestIdDto,
                               final ComponentType requestingComponentType,
                               final String requestingComponentId,
                               final ComponentType respondingComponentType,
                               final String respondingComponentId,
                               final String name) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(requestingComponentId)
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(respondingComponentId)
                .respondingComponentCountry(gateProperties.getCountry()).build();

        final String body = serializeUtils.mapObjectToBase64String(requestIdDto);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, control.getStatus(), false, name);
    }

    public void logPlatformResponse(final NotificationDto notificationDto, final UilRequestDto uilRequestDto) {
        final NotificationContentDto content = notificationDto.getContent();
        final ControlDto control = uilRequestDto.getControl();

        this.logReceivedMessage(control, PLATFORM, GATE, content.getBody(), content.getFromPartyId(), REQUEST_STATUS_ENUM_STATUS_ENUM_MAP.getOrDefault(uilRequestDto.getStatus(), COMPLETE), LogManager.FTI_010);
        final String currentGateId = gateProperties.getOwner();
        final String currentGateCountry = gateProperties.getCountry();
        final String platformId = control.getPlatformId();
        reportingRequestLogService.logReportingRequest(control, uilRequestDto, currentGateId, currentGateCountry, RequestTypeLog.UIL, GATE, currentGateId, currentGateCountry, PLATFORM, platformId, currentGateCountry, true);
    }

    public void logSentMessage(final RequestDto request, boolean isSuccess) {
        this.logAckMessage(request.getControl(), GATE, true, request, isSuccess ? FTI_ROOT_RESPONSE_SUCESS : FTI_SEND_FAIL);
        if (List.of(EXTERNAL_ASK_IDENTIFIERS_SEARCH, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH).contains(request.getControl().getRequestType())
        && !request.getStatus().equals(RequestStatusEnum.IN_PROGRESS)) {
            final ControlDto controlDto = request.getControl();
            final RequestTypeLog requestTypeLog = controlDto.getRequestType() == EXTERNAL_ASK_IDENTIFIERS_SEARCH ? RequestTypeLog.IDENTIFIERS : RequestTypeLog.UIL;
            final String currentGateCountry = gateProperties.getCountry();
            reportingRequestLogService.logReportingRequest(controlDto, request, gateProperties.getOwner(), currentGateCountry, requestTypeLog, GATE, controlDto.getFromGateId(), eftiGateIdResolver.resolve(controlDto.getFromGateId()), GATE, gateProperties.getOwner(), currentGateCountry, false);
        }
        if (RequestType.NOTE.equals(request.getRequestType())) {
            final ControlDto controlDto = request.getControl();
            final String currentGateCountry = gateProperties.getCountry();
            reportingRequestLogService.logReportingRequest(controlDto, request, gateProperties.getOwner(), currentGateCountry, RequestTypeLog.NOTE, GATE, gateProperties.getOwner(), currentGateCountry, PLATFORM, controlDto.getPlatformId(), currentGateCountry, false);

        }
    }
}
