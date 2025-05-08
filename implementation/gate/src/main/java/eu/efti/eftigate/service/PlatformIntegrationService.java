package eu.efti.eftigate.service;

import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.RabbitRequestDto;
import eu.efti.eftigate.service.request.NotesRequestService;
import eu.efti.eftigate.service.request.UilRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PlatformIntegrationService {
    private final List<GateProperties.PlatformProperties> platformsProperties;

    private final UilRequestService uilRequestService;

    private final NotesRequestService notesRequestService;

    private final PlatformRestService platformRestService;

    private final DomibusIntegrationService domibusIntegrationService;

    private Optional<GateProperties.PlatformProperties> getPlatformProperties(String platformId) {
        return platformsProperties.stream()
                .filter(platformProperties -> platformProperties.platformId().equals(platformId))
                .findFirst();
    }

    @Autowired
    public PlatformIntegrationService(GateProperties gateProperties, UilRequestService uilRequestService, NotesRequestService notesRequestService, PlatformRestService platformRestService, DomibusIntegrationService domibusIntegrationService) {
        this.platformsProperties = gateProperties.getPlatforms();
        this.uilRequestService = uilRequestService;
        this.notesRequestService = notesRequestService;
        this.platformRestService = platformRestService;
        this.domibusIntegrationService = domibusIntegrationService;
    }

    public boolean platformExists(String platformId) {
        return getPlatformProperties(platformId).isPresent();
    }

    public record PlatformInfo(boolean useRestApi, URI restApiBaseUrl) {
    }

    Optional<PlatformInfo> getPlatformInfo(String platformId) {
        return getPlatformProperties(platformId)
                .map(platformProperties -> new PlatformInfo(platformProperties.restApiBaseUrl() != null, platformProperties.restApiBaseUrl()));
    }

    void handle(final RabbitRequestDto rabbitRequestDto) {
        final ControlDto control = rabbitRequestDto.getControl();
        final RequestTypeEnum requestTypeEnum = control.getRequestType();
//        final ComponentType target = gateProperties.isCurrentGate(rabbitRequestDto.getGateIdDest()) ? ComponentType.PLATFORM : ComponentType.GATE;

//         TODO: assertion: if (ComponentType.PLATFORM.equals(target)
        if (getPlatformInfo(control.getPlatformId()).map(PlatformInfo::useRestApi).orElse(false)) {
            var platformId = control.getPlatformId();
            var platformInfo = getPlatformInfo(platformId);
            if (platformInfo.isEmpty()) {
                throw new IllegalArgumentException("platform " + platformId + " does not exist");
            }
            var client = platformRestService.getClient(platformInfo.get().restApiBaseUrl());
            try {
                if (RequestTypeEnum.LOCAL_UIL_SEARCH.equals(requestTypeEnum)) {
                    uilRequestService.manageRestRequestInProgress(control.getRequestId());
                    var res = client.callGetConsignmentSubsets(control.getDatasetId(), Set.copyOf(control.getSubsetIds()));
                    uilRequestService.manageRestResponseReceived(control.getRequestId(), res);
                } else if (RequestTypeEnum.NOTE_SEND.equals(requestTypeEnum)) {
                    notesRequestService.manageRestRequestInProgress(control.getRequestId());
                    client.callPostConsignmentFollowup(control.getDatasetId(), rabbitRequestDto.getNote());
                    notesRequestService.manageRestRequestDone(control.getRequestId());
                } else {
                    throw new TechnicalException("unexpected request type: " + requestTypeEnum);
                }
            } catch (PlatformIntegrationServiceException e) {
                throw new RuntimeException(e);
            }
        } else {
            domibusIntegrationService.trySendDomibus(rabbitRequestDto, rabbitRequestDto.getControl());
        }
    }
}
