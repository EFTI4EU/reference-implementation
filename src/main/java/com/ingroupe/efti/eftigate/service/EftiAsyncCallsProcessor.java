package com.ingroupe.efti.eftigate.service;

import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.IdentifiersRequestDto;
import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.eftigate.service.request.MetadataRequestService;
import com.ingroupe.efti.metadataregistry.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor()
@Slf4j
public class EftiAsyncCallsProcessor {
    public static final String FTI_015 = "fti015";
    public static final String FTI_016 = "fti016";
    private final MetadataRequestService metadataRequestService;
    private final MetadataService metadataService;
    private final LogManager logManager;

    @Async
    public void checkLocalRepoAsync(final MetadataRequestDto metadataRequestDto, final ControlDto savedControl) {
        //juju commentaire fti015
        logManager.logRegistryMetadata(savedControl, null, FTI_015);
        final List<MetadataDto> metadataDtoList = metadataService.search(metadataRequestDto);
        //juju commentaire fti016
        logManager.logRegistryMetadata(savedControl, metadataDtoList, FTI_016);
        metadataRequestService.createRequest(savedControl, RequestStatusEnum.SUCCESS, metadataDtoList);
    }
}



