package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.feign.PlatformClient;
import eu.efti.eftigate.service.gate.EftiPlatformIdResolver;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.UilRequestService;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
@Slf4j
public class PlatformIntegrationService {
    private final PlatformClient platformClient;
    private final UilRequestService uilRequestService;
    private final NotesRequestService notesRequestService;
    private final SerializeUtils serializeUtils;
    private final EftiPlatformIdResolver eftiPlatformIdResolver;


    void handle(final RabbitRequestDto rabbitRequestDto) {
        ControlDto control = rabbitRequestDto.getControl();
        String requestId = control.getRequestId();
        String platformUrlByPlatformId = eftiPlatformIdResolver.getPlatformUrlByPlatformId(control.getPlatformId());
        if (StringUtils.isNotBlank(platformUrlByPlatformId)) {
            URI baseUri = URI.create(platformUrlByPlatformId);
            if (RequestType.NOTE.equals(rabbitRequestDto.getRequestType())) {
                notesRequestService.manageRestRequestInProgress(requestId);
                platformClient.postConsignmentFollowUp(baseUri, control.getDatasetId(), rabbitRequestDto.getNote());
                notesRequestService.manageRestRequestDone(requestId);
            } else {
                uilRequestService.manageRestRequestInProgress(requestId);
                SupplyChainConsignment supplyChainConsignment = new SupplyChainConsignment();
                ResponseEntity<String> response = platformClient.sendUilQuery(baseUri, control.getDatasetId(), control.getSubsetIds());
                String body = response.getBody();
                if (StringUtils.isNotBlank(body)) {
                    supplyChainConsignment = serializeUtils.mapXmlStringToJaxbObject(body, SupplyChainConsignment.class);
                }
                uilRequestService.manageRestResponseReceivedFromPlatform(requestId, supplyChainConsignment);
            }
        } else {
            throw new TechnicalException("Url not found for platform with Id: " + control.getPlatformId());
        }
    }
}
