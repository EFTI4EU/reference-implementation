package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
        final List<IdentifiersDto> identifiersDtoList = identifiersService.search(identifiersRequestDto);
        logManager.logLocalRegistryMessage(savedControl, identifiersDtoList);
        final IdentifiersRequestDto request = identifiersRequestService.createRequest(savedControl, RequestStatusEnum.SUCCESS, identifiersDtoList);
        if (shouldUpdateControl(savedControl, request, identifiersDtoList)) {
            identifiersRequestService.updateControlIdentifiers(request.getControl(), identifiersDtoList);
        }
    }

    private static boolean shouldUpdateControl(final ControlDto savedControl, final IdentifiersRequestDto request, final List<IdentifiersDto> identifiersDtoList) {
        return request != null && RequestStatusEnum.SUCCESS.equals(request.getStatus())
                && CollectionUtils.isNotEmpty(identifiersDtoList)
                && RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH.equals(savedControl.getRequestType());
    }
}
