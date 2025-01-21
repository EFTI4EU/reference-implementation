package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.ValidableDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.service.gate.EftiGateIdResolver;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogManager {

    private final GateProperties gateProperties;
    private final EftiGateIdResolver eftiGateIdResolver;
    private final AuditRequestLogService auditRequestLogService;
    private final AuditRegistryLogService auditRegistryLogService;
    private final SerializeUtils serializeUtils;

    public static final String FTI_ROOT_RESPONSE_SUCESS = "send sucess to domibus";
    public static final String FTI_SEND_FAIL = "send fail to domibus";
    public static final String FTI_008_FTI_014 = "fti008|fti014";
    public static final String FTI_015 = "fti015";
    public static final String FTI_016 = "fti016";
    public static final String FTI_011_FTI_017 = "fti011|fti017";
    public static final String FTI_022 = "fti022";
    public static final String FTI_010 = "fti010";
    public static final String FTI_009_FTI_020 = "fti009|fti020";
    public static final String FTI_021 = "fti021";
    public static final String FTI_019 = "fti019";
    public static final String FTI_023 = "fti023";
    public static final String FTI_025 = "fti025";
    public static final String FTI_026 = "fti026";

    public void logNoteReceiveFromAapMessage(final ControlDto control,
                                             final String message,
                                             final String receiver,
                                             final ComponentType requestingComponentType,
                                             final ComponentType respondingComponentType,
                                             final boolean isSuccess,
                                             final RequestTypeEnum requestType,
                                             final String name) {
        String receiverCountry = eftiGateIdResolver.resolve(receiver);
        control.setRequestType(requestType);
        sendLogRequest(control, message, receiver, requestingComponentType, respondingComponentType, isSuccess, name, receiverCountry);
    }

    private void sendLogRequest(ControlDto control, String message, String receiver, ComponentType requestingComponentType, ComponentType respondingComponentType, boolean isSuccess, String name, String receiverCountry) {
        final MessagePartiesDto messagePartiesDto = buildMessagePartiesDto(receiver, requestingComponentType, respondingComponentType, receiverCountry);
        final StatusEnum status = isSuccess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        final String body = serializeUtils.mapObjectToBase64String(message);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false, name);
    }

    private MessagePartiesDto buildMessagePartiesDto(String receiver, ComponentType requestingComponentType, ComponentType respondingComponentType, String receiverCountry) {
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
                              final ComponentType requestingComponentType,
                              final ComponentType respondingComponentType,
                              final boolean isSuccess,
                              final String name) {
        //todo not working for gate to gate, need to find a way to find the receiver
        final boolean isLocalRequest = control.getRequestType() == RequestTypeEnum.LOCAL_UIL_SEARCH;
        final String receiver = isLocalRequest ? control.getPlatformId() : control.getGateId();
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(requestingComponentType)
                .requestingComponentId(receiver)
                .requestingComponentCountry(isLocalRequest ? gateProperties.getCountry() : eftiGateIdResolver.resolve(receiver))
                .respondingComponentType(respondingComponentType)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
        final StatusEnum status = isSuccess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), "", status, true, name);
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

    public void logRegistryIdentifiers(final ControlDto control,
                                       final List<ConsignmentDto> consignementList,
                                       final ComponentType requestingComponentType,
                                       final ComponentType respondingComponentType,
                                       final String name) {
        final String body = consignementList != null ? serializeUtils.mapObjectToBase64String(consignementList) : null;
        this.auditRegistryLogService.logByControlDto(control, gateProperties.getOwner(), gateProperties.getCountry(), requestingComponentType, respondingComponentType, body, null, name);
    }

    public void logLocalIdentifierMessage(final ControlDto control,
                                          final IdentifiersResponseDto identifierRequestResultDtos,
                                          ComponentType requestingComponentType, ComponentType respondingComponentType, final String name) {
        final MessagePartiesDto messagePartiesDto = getMessagePartiesDto(requestingComponentType, respondingComponentType);
        final String body = serializeUtils.mapObjectToBase64String(identifierRequestResultDtos);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false, name);
    }

    private MessagePartiesDto getMessagePartiesDto(ComponentType requestingComponentType, ComponentType respondingComponentType) {
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
        this.auditRegistryLogService.logByControlDto(controlDto, gateProperties.getOwner(), gateProperties.getCountry(), requestingComponentType, respondingComponentType, body, null, name);
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

}
