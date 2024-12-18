package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.identifiers.ConsignmentDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

import static eu.efti.eftilogger.model.ComponentType.GATE;
import static eu.efti.eftilogger.model.ComponentType.REGISTRY;

@Component
@RequiredArgsConstructor()
@Slf4j
public class EftiAsyncCallsProcessor {
    private final IdentifiersRequestService identifiersRequestService;
    private final IdentifiersService identifiersService;
    private final LogManager logManager;

    @Async
    public void checkLocalRepoAsync(final SearchWithIdentifiersRequestDto identifiersRequestDto, final ControlDto savedControl) {
        //log fti015
        logManager.logRegistryIdentifiers(savedControl, null, GATE, REGISTRY, LogManager.FTI_015);
        final List<ConsignmentDto> metadataDtoList = identifiersService.search(identifiersRequestDto);
        //logfti016
        logManager.logRegistryIdentifiers(savedControl, metadataDtoList, REGISTRY, GATE, LogManager.FTI_016);
        identifiersRequestService.createRequest(savedControl, RequestStatusEnum.SUCCESS, metadataDtoList);
        if (savedControl.isLocalAsk()) {
            identifiersRequestService.updateControl(savedControl);
        }
    }
}
