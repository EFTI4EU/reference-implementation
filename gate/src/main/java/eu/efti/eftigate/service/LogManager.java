package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.ValidableDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RequestUuidDto;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.service.gate.EftiGateUrlResolver;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.service.AuditRegistryLogService;
import eu.efti.eftilogger.service.AuditRequestLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static eu.efti.eftilogger.model.ComponentType.CA_APP;
import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.PLATFORM;

@Service
@RequiredArgsConstructor
public class LogManager {

    private final GateProperties gateProperties;
    private final EftiGateUrlResolver eftiGateUrlResolver;
    private final AuditRequestLogService auditRequestLogService;
    private final AuditRegistryLogService auditRegistryLogService;
    private final SerializeUtils serializeUtils;
    private final MapperUtils mapperUtils;

    public static final String FTI_ROOT_RESPONSE_SUCESS = "fti root response sucess";
    public static final String FTI_SEND_FAIL = "fti send fail";
    public static final String FTI_008_FTI_014 = "fti008|fti014";
    public static final String FTI_015 = "fti015";
    public static final String FTI_016 = "fti016";
    public static final String LOG_FROM_IDENTIFIERS_REQUEST_DTO = "logFromIdentifiersRequestDto";
    public static final String FTI_017 = "fti017";
    public static final String FTI_010_FTI_022_ET_AUTRES = "fti010, fti 022 et autres";
    public static final String FTI_022_FTI_010 = "fti022|fti010";
    public static final String UIL_FTI_020_FTI_009 = "uil|FTI020|fti009";
    public static final String IDENTIFIERS = "identifiers";

    public void logRequestForIdentifiers(ControlDto controlDto, String body, String currentGateId, String currentGateCountry, String errorCode, String name) {
        auditRegistryLogService.logByControlDto(controlDto, currentGateId, currentGateCountry, body, errorCode, name);
    }

    public void logSentMessage(final ControlDto control,
                               final String message,
                               final String receiver,
                               final boolean isCurrentGate,
                               final boolean isSucess,
                               final String name) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(ComponentType.GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(isCurrentGate ? ComponentType.PLATFORM : ComponentType.GATE)
                .respondingComponentId(receiver)
                .respondingComponentCountry(eftiGateUrlResolver.resolve(receiver)).build();
        final StatusEnum status = isSucess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        final String body = serializeUtils.mapObjectToBase64String(message);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false, name);
    }

    public void logFromIdentifier(IdentifiersResponseDto identifiersResponseDto, ControlDto controlDto, final String name) {
        List<IdentifiersDto> identifiersDtoList = new ArrayList<>();
        identifiersResponseDto.getIdentifiers().forEach(mapperUtils::identifiersResultDtoToIdentifiersDto);
        this.logLocalRegistryMessage(controlDto, identifiersDtoList, name);
    }

    public void logFromIdentifiersRequestDto(ControlDto controlDto, SearchWithIdentifiersRequestDto identifiersRequestDto, final boolean isCurrentGate, final String receiver, final boolean isSucess, final boolean isAck, final String name) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(ComponentType.GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(isCurrentGate ? ComponentType.PLATFORM : ComponentType.GATE)
                .respondingComponentId(receiver)
                .respondingComponentCountry(eftiGateUrlResolver.resolve(receiver)).build();
        final String body = serializeUtils.mapObjectToBase64String(identifiersRequestDto);
        final StatusEnum status = isSucess ? StatusEnum.COMPLETE : StatusEnum.ERROR;

        auditRequestLogService.log(controlDto, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, isAck, name);
    }

    public void logAckMessage(final ControlDto control,
                              final boolean isSucess,
                              final String name) {
        //todo not working for gate to gate, need to find a way to find the receiver
        final boolean isLocalRequest = control.getRequestType() == RequestTypeEnum.LOCAL_UIL_SEARCH;
        final String receiver = isLocalRequest ? control.getEftiPlatformUrl() : control.getEftiGateUrl();
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(isLocalRequest ? PLATFORM : GATE)
                .requestingComponentId(receiver)
                .requestingComponentCountry(isLocalRequest ? gateProperties.getCountry() : eftiGateUrlResolver.resolve(receiver))
                .respondingComponentType(GATE)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
        final StatusEnum status = isSucess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), "", status, true, name);
    }

    public void logReceivedMessage(final ControlDto control,
                                   final String body,
                                   final String sender,
                                   final String name) {
        final String senderCountry = eftiGateUrlResolver.resolve(sender);
        final boolean senderIsKnown = senderCountry != null;
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(senderIsKnown ? GATE : PLATFORM) // if sender is unknown, its a platform
                .requestingComponentId(sender)
                .requestingComponentCountry(senderIsKnown ? senderCountry : gateProperties.getCountry())
                .respondingComponentType(GATE)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
        final String bodyBase64 = serializeUtils.mapObjectToBase64String(body);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), bodyBase64, StatusEnum.COMPLETE, false, name);
    }

    public void logRegistryIdentifiers(final ControlDto control,
                                       final List<IdentifiersDto> metadataDtoList,
                                       final String name) {
        final String body = metadataDtoList != null ? serializeUtils.mapObjectToBase64String(metadataDtoList) : null;
        this.auditRegistryLogService.logByControlDto(control, gateProperties.getOwner(), gateProperties.getCountry(), body, null, name);
    }

    public void logLocalRegistryMessage(final ControlDto control,
                                        final List<IdentifiersDto> identifiersDtoList,
                                        final String name) {
        final MessagePartiesDto messagePartiesDto = getMessagePartiesDto();
        final String body = serializeUtils.mapObjectToBase64String(identifiersDtoList);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false, name);
    }

    private MessagePartiesDto getMessagePartiesDto() {
        return MessagePartiesDto.builder()
                .requestingComponentType(GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(GATE)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
    }

    public void logRequestRegistry(final ControlDto controlDto, final String body, final String name) {
        this.auditRegistryLogService.logByControlDto(controlDto, gateProperties.getOwner(), gateProperties.getCountry(), body, null, name);
    }

    public <T extends ValidableDto> void logAppRequest(final ControlDto control,
                                                       final T searchDto,
                                                       final String name) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(CA_APP)
                .requestingComponentId("")
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(GATE)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();

        final String body = serializeUtils.mapObjectToBase64String(searchDto);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false, name);
    }

    public void logAppResponse(final ControlDto control,
                               final RequestUuidDto requestUuidDto,
                               final String name) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(CA_APP)
                .respondingComponentId("")
                .respondingComponentCountry(gateProperties.getCountry()).build();

        final String body = serializeUtils.mapObjectToBase64String(requestUuidDto);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false, name);
    }

}
