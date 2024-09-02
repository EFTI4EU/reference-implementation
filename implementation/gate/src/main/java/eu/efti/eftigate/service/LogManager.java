package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.commons.dto.ValidableDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RequestUuidDto;
import eu.efti.eftigate.service.gate.EftiGateUrlResolver;
import eu.efti.eftilogger.dto.MessagePartiesDto;
import eu.efti.eftilogger.model.ComponentType;
import eu.efti.eftilogger.service.AuditRequestLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final SerializeUtils serializeUtils;

    public void logSentMessage(final ControlDto control,
                               final String message,
                               final String receiver,
                               final boolean isCurrentGate,
                               final boolean isSucess) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(ComponentType.GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(isCurrentGate? ComponentType.PLATFORM : ComponentType.GATE)
                .respondingComponentId(receiver)
                .respondingComponentCountry(eftiGateUrlResolver.resolve(receiver)).build();
        final StatusEnum status = isSucess ? StatusEnum.COMPLETE : StatusEnum.ERROR;
        final String body = serializeUtils.mapObjectToBase64String(message);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, status, false);
    }

    public void logAckMessage(final ControlDto control,
                              final boolean isSucess) {
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
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), "", status, true);
    }

    public void logReceivedMessage(final ControlDto control,
                                   final String body,
                                   final String sender) {
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
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), bodyBase64, StatusEnum.COMPLETE, false);
    }

    public void logLocalRegistryMessage(final ControlDto control,
                                        final List<ConsignmentIdentifiersDTO> consignmentIdentifiersDTOS) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(GATE)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();
        final String body = serializeUtils.mapObjectToBase64String(consignmentIdentifiersDTOS);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false);
    }

    public <T extends ValidableDto> void logAppRequest(final ControlDto control,
                                                  final T searchDto) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(CA_APP)
                .requestingComponentId("")
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(GATE)
                .respondingComponentId(gateProperties.getOwner())
                .respondingComponentCountry(gateProperties.getCountry()).build();

        final String body = serializeUtils.mapObjectToBase64String(searchDto);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false);
    }

    public void logAppResponse(final ControlDto control,
                               final RequestUuidDto requestUuidDto) {
        final MessagePartiesDto messagePartiesDto = MessagePartiesDto.builder()
                .requestingComponentType(GATE)
                .requestingComponentId(gateProperties.getOwner())
                .requestingComponentCountry(gateProperties.getCountry())
                .respondingComponentType(CA_APP)
                .respondingComponentId("")
                .respondingComponentCountry(gateProperties.getCountry()).build();

        final String body = serializeUtils.mapObjectToBase64String(requestUuidDto);
        this.auditRequestLogService.log(control, messagePartiesDto, gateProperties.getOwner(), gateProperties.getCountry(), body, StatusEnum.COMPLETE, false);
    }

}
