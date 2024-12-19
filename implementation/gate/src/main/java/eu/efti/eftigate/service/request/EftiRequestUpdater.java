package eu.efti.eftigate.service.request;

import eu.efti.commons.constant.EftiGateConstants;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.repository.RequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.LogManager;
import eu.efti.eftilogger.model.ComponentType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static eu.efti.commons.enums.RequestStatusEnum.SEND_ERROR;

@Slf4j
@Component
@AllArgsConstructor
public class EftiRequestUpdater {
    public static final String COULDN_T_FIND_REQUEST_FOR_MESSAGE_ID = "couldn't find request for messageId: ";
    public static final String SENT_MESSAGE_SUCCESSFULLY_BUT_NOT_SAVED_IN_DB = "sent message {} successfully, but not saved in DB";
    public static final String SENT_MESSAGE_SUCCESSFULLY = "sent message {} successfully";

    private final RequestRepository<?> requestRepository;
    private final ControlService controlService;
    private final RequestServiceFactory requestServiceFactory;
    private final LogManager logManager;
    private final MapperUtils mapperUtils;


    public void manageSendFailure(final NotificationDto notificationDto, final String name) {
        final Optional<RequestDto> requestDto = getRequestDtoFromMessageId(notificationDto.getMessageId());
        if (requestDto.isPresent()) {
            this.updateStatus(requestDto.get(), SEND_ERROR);
            logManager.logAckMessage(requestDto.get().getControl(), null, null, false, name);
        }
    }

    public void manageSendSuccess(final NotificationDto notificationDto, final String name) {
        final Optional<RequestDto> requestDto = getRequestDtoFromMessageId(notificationDto.getMessageId());
        if (requestDto.isEmpty()) {
            log.info(SENT_MESSAGE_SUCCESSFULLY_BUT_NOT_SAVED_IN_DB, notificationDto.getMessageId());
            return;
        }
        if (List.of(RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, RequestTypeEnum.EXTERNAL_UIL_SEARCH, RequestTypeEnum.LOCAL_UIL_SEARCH).contains(requestDto.get().getControl().getRequestType())) {
            getRequestService(requestDto.get().getRequestType().name()).manageSendSuccess(notificationDto.getMessageId());
        } else {
            log.info(SENT_MESSAGE_SUCCESSFULLY, notificationDto.getMessageId());
        }
        logManager.logAckMessage(requestDto.get().getControl(), ComponentType.GATE, null, true, name);
    }

    private RequestService<?> getRequestService(final String requestType) {
        return requestServiceFactory.getRequestServiceByRequestType(requestType);
    }

    private Optional<RequestDto> getRequestDtoFromMessageId(final String messageId) {
        final Optional<RequestEntity> request = findRequestByMessageId(messageId);
        if (request.isPresent()) {
            RequestType requestType = RequestType.valueOf(request.get().getRequestType());
            return Optional.ofNullable(mapperUtils.requestToRequestDto(request.get(), EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(requestType)));
        }
        return Optional.empty();
    }


    protected Optional<RequestEntity> findRequestByMessageId(final String messageId) {
        Optional<RequestEntity> requestEntity = Optional.ofNullable(this.requestRepository.findByEdeliveryMessageId(messageId));
        if (requestEntity.isPresent()) {
            return requestEntity;
        } else {
            log.error(COULDN_T_FIND_REQUEST_FOR_MESSAGE_ID + messageId);
            return Optional.empty();
        }
    }

    public void updateStatus(final RequestDto request, final RequestStatusEnum status) {
        request.setStatus(status);
        controlService.save(request.getControl());
    }
}
