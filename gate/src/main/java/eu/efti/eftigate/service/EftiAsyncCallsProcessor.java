package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.MetadataDto;
import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.eftigate.service.request.MetadataRequestService;
import eu.efti.metadataregistry.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor()
@Slf4j
public class EftiAsyncCallsProcessor {

    private final MetadataRequestService metadataRequestService;
    private final MetadataService metadataService;
    private final LogManager logManager;

    @Async
    public void checkLocalRepoAsync(final MetadataRequestDto metadataRequestDto, final ControlDto savedControl) {
        //log fti015
        logManager.logRegistryMetadata(savedControl, null, LogManager.FTI_015);
        final List<MetadataDto> metadataDtoList = metadataService.search(metadataRequestDto);
        //logfti016
        logManager.logRegistryMetadata(savedControl, metadataDtoList, LogManager.FTI_016);
        metadataRequestService.createRequest(savedControl, RequestStatusEnum.SUCCESS, metadataDtoList);
    }
}



