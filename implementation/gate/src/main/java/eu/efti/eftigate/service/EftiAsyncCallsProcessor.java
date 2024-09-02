package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.commons.dto.ConsignmentIdentifiersRequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
import eu.efti.identifierregistry.service.IdentifiersService;
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
    public void checkLocalRepoAsync(final ConsignmentIdentifiersRequestDto consignmentIdentifiersRequestDto, final ControlDto savedControl) {
        final List<ConsignmentIdentifiersDTO> consignmentIdentifiersDTOList = identifiersService.search(consignmentIdentifiersRequestDto);
        logManager.logLocalRegistryMessage(savedControl, consignmentIdentifiersDTOList);
        final IdentifiersRequestDto request = identifiersRequestService.createRequest(savedControl, RequestStatusEnum.SUCCESS, consignmentIdentifiersDTOList);
        if (shouldUpdateControl(savedControl, request, consignmentIdentifiersDTOList)) {
            identifiersRequestService.updateControlIdentifiers(request.getControl(), consignmentIdentifiersDTOList);
        }
    }

    private static boolean shouldUpdateControl(final ControlDto savedControl, final IdentifiersRequestDto request, final List<ConsignmentIdentifiersDTO> consignmentIdentifiersDTOList) {
        return request != null && RequestStatusEnum.SUCCESS.equals(request.getStatus())
                && CollectionUtils.isNotEmpty(consignmentIdentifiersDTOList)
                && RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH.equals(savedControl.getRequestType());
    }
}
