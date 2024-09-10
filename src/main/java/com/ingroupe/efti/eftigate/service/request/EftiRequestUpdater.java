package com.ingroupe.efti.eftigate.service.request;

import com.ingroupe.efti.commons.constant.EftiGateConstants;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.eftigate.entity.RequestEntity;
import com.ingroupe.efti.eftigate.exception.RequestNotFoundException;
import com.ingroupe.efti.eftigate.mapper.MapperUtils;
import com.ingroupe.efti.eftigate.repository.RequestRepository;
import com.ingroupe.efti.eftigate.service.ControlService;
import com.ingroupe.efti.eftigate.service.LogManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.ingroupe.efti.commons.enums.RequestStatusEnum.SEND_ERROR;

@Slf4j
@Component
@AllArgsConstructor
public class EftiRequestUpdater {
    private final RequestRepository<?> requestRepository;
    private final ControlService controlService;
    private final RequestServiceFactory requestServiceFactory;
    private final LogManager logManager;
    private final MapperUtils mapperUtils;


    public void manageSendFailure(final NotificationDto notificationDto, final String name) {
        final RequestDto requestDto = getRequestDtoFromMessageId(notificationDto.getMessageId());
        this.updateStatus(requestDto, SEND_ERROR);
        //logManager.logAckMessage(requestDto.getControl(), false, name);
    }

    public void manageSendSuccess(final NotificationDto notificationDto, final String name) {
        final RequestDto requestDto = getRequestDtoFromMessageId(notificationDto.getMessageId());
        if (List.of(RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH, RequestTypeEnum.EXTERNAL_UIL_SEARCH, RequestTypeEnum.LOCAL_UIL_SEARCH).contains(requestDto.getControl().getRequestType())) {
            getRequestService(requestDto.getRequestType().name()).manageSendSuccess(notificationDto.getMessageId());
        } else {
            log.info(" sent message {} successfully", notificationDto.getMessageId());
        }
        //logManager.logAckMessage(requestDto.getControl(), true, name);
    }

    private RequestService<?> getRequestService(final String requestType) {
        return  requestServiceFactory.getRequestServiceByRequestType(requestType);
    }

    private RequestDto getRequestDtoFromMessageId(final String messageId) {
        final RequestEntity request = findRequestByMessageIdOrThrow(messageId);
        return mapperUtils.requestToRequestDto(request, EftiGateConstants.REQUEST_TYPE_CLASS_MAP.get(RequestType.valueOf(request.getRequestType())));
    }


    protected RequestEntity findRequestByMessageIdOrThrow(final String messageId) {
        return Optional.ofNullable(this.requestRepository.findByEdeliveryMessageId(messageId))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find request for messageId: " + messageId));
    }

    public void updateStatus(final RequestDto request, final RequestStatusEnum status) {
        request.setStatus(status);
        controlService.save(request.getControl());
    }
}
