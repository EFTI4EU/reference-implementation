package eu.efti.eftigate.service.request;

import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.v1.edelivery.IdentifierResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This service is created to serve as proxy , so that @transactional annotation for identifiers works correctly
 */
@Service
public class IdentifiersControlUpdateDelegateService {
    private final IdentifiersRequestRepository identifiersRequestRepository;
    private final ControlService controlService;
    private final MapperUtils mapperUtils;

    public IdentifiersControlUpdateDelegateService(final IdentifiersRequestRepository identifiersRequestRepository, final ControlService controlService, final MapperUtils mapperUtils) {
        this.identifiersRequestRepository = identifiersRequestRepository;
        this.controlService = controlService;
        this.mapperUtils = mapperUtils;
    }

    @Transactional("controlTransactionManager")
    public void updateExistingControl(final IdentifierResponse response, final String gateUrlDest) {
        final IdentifiersResults identifiersResults = IdentifiersResults.builder().consignments(mapperUtils.eDeliveryToDto(response.getConsignment())).build();
        final IdentifiersRequestEntity waitingRequest = identifiersRequestRepository.findByControlRequestIdAndStatusAndGateUrlDest(response.getRequestId(), RequestStatusEnum.IN_PROGRESS, gateUrlDest);
        if (waitingRequest != null) {
            updateControlRequests(waitingRequest, identifiersResults);
        }
    }

    @Transactional("controlTransactionManager")
    public void setControlNextStatus(final String controlRequestId) {
        final List<RequestStatusEnum> requestStatuses = identifiersRequestRepository.findByControlRequestId(controlRequestId).stream()
                .map(RequestEntity::getStatus)
                .toList();
        controlService.findByRequestId(controlRequestId).ifPresent(controlEntity -> {
            if (!StatusEnum.ERROR.equals(controlEntity.getStatus())) {
                final StatusEnum controlStatus = getControlNextStatus(controlEntity.getStatus(), requestStatuses);
                controlEntity.setStatus(controlStatus);
                controlService.save(controlEntity);
            }
        });
    }

    private StatusEnum getControlNextStatus(final StatusEnum currentStatus, final List<RequestStatusEnum> requestStatuses) {
        if (requestStatuses.stream().allMatch(requestStatusEnum -> RequestStatusEnum.SUCCESS == requestStatusEnum)) {
            return StatusEnum.COMPLETE;
        } else if (requestStatuses.stream().anyMatch(requestStatusEnum -> RequestStatusEnum.TIMEOUT == requestStatusEnum)
                && requestStatuses.stream().noneMatch(requestStatusEnum -> RequestStatusEnum.ERROR == requestStatusEnum)) {
            return StatusEnum.TIMEOUT;
        } else if (requestStatuses.stream().anyMatch(requestStatusEnum -> RequestStatusEnum.ERROR == requestStatusEnum)) {
            return StatusEnum.ERROR;
        }
        return currentStatus;
    }

    private void updateControlRequests(final IdentifiersRequestEntity waitingRequest, final IdentifiersResults identifiersResults) {
        waitingRequest.setIdentifiersResults(identifiersResults);
        waitingRequest.setStatus(RequestStatusEnum.SUCCESS);
        identifiersRequestRepository.save(waitingRequest);
    }
}
