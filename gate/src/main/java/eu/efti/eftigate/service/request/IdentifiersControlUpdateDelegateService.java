package eu.efti.eftigate.service.request;

import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.IdentifiersResultDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.entity.IdentifiersRequestEntity;
import eu.efti.eftigate.entity.IdentifiersResult;
import eu.efti.eftigate.entity.IdentifiersResults;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.IdentifiersRequestRepository;
import eu.efti.eftigate.service.ControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This service is created to serve as proxy , so that @transactional annotation for identifiers works correctly
 */
@Service
public class IdentifiersControlUpdateDelegateService {
    private final IdentifiersRequestRepository identifiersRequestRepository;
    private final SerializeUtils serializeUtils;
    private final ControlService controlService;
    private final MapperUtils mapperUtils;

    public IdentifiersControlUpdateDelegateService(final IdentifiersRequestRepository identifiersRequestRepository, final SerializeUtils serializeUtils, final ControlService controlService, final MapperUtils mapperUtils) {
        this.identifiersRequestRepository = identifiersRequestRepository;
        this.serializeUtils = serializeUtils;
        this.controlService = controlService;
        this.mapperUtils = mapperUtils;
    }

    @Transactional("controlTransactionManager")
    public void updateExistingControl(final String bodyFromNotification, final String requestUuid, final String gateUrlDest) {
        final IdentifiersResponseDto response = serializeUtils.mapXmlStringToClass(bodyFromNotification, IdentifiersResponseDto.class);
        final List<IdentifiersResultDto> identifiersResultDtoList = response.getIdentifiers();
        final IdentifiersResults identifiersResults = buildIdentifiersResultFrom(identifiersResultDtoList);
        final IdentifiersRequestEntity waitingRequest = identifiersRequestRepository.findByControlRequestUuidAndStatusAndGateUrlDest(requestUuid, RequestStatusEnum.IN_PROGRESS, gateUrlDest);
        if (waitingRequest != null) {
            updateControlRequests(waitingRequest, identifiersResults);
        }
    }

    @Transactional("controlTransactionManager")
    public void setControlNextStatus(final String controlRequestUuid) {
        final List<RequestStatusEnum> requestStatuses = identifiersRequestRepository.findByControlRequestUuid(controlRequestUuid).stream()
                .map(RequestEntity::getStatus)
                .toList();
        controlService.findByRequestUuid(controlRequestUuid).ifPresent(controlEntity -> {
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

    private IdentifiersResults buildIdentifiersResultFrom(final List<IdentifiersResultDto> identifiersResultDtoList) {
        final List<IdentifiersResult> identifiersResultList = mapperUtils.identifierResultDtosToIdentifierEntities(identifiersResultDtoList);
        return IdentifiersResults.builder()
                .identifiersResult(identifiersResultList)
                .build();
    }
}
