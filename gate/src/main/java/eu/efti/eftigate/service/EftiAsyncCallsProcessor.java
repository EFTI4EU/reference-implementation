package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

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
        logManager.logRegistryIdentifiers(savedControl, null, LogManager.FTI_015);
        final List<IdentifiersDto> metadataDtoList = identifiersService.search(identifiersRequestDto);
        //logfti016
        logManager.logRegistryIdentifiers(savedControl, metadataDtoList, LogManager.FTI_016);
        identifiersRequestService.createRequest(savedControl, RequestStatusEnum.SUCCESS, metadataDtoList);
    }
}
