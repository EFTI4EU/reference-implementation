package eu.efti.eftigate.service.request;

import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.commons.enums.ErrorCodesEnum;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestTypeEnum;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.edeliveryapconnector.dto.MessageBodyDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.service.RequestUpdaterService;
import eu.efti.eftigate.config.GateProperties;
import eu.efti.eftigate.dto.ControlDto;
import eu.efti.eftigate.dto.ErrorDto;
import eu.efti.eftigate.dto.RequestDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.entity.RequestEntity;
import eu.efti.eftigate.exception.RequestNotFoundException;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.eftigate.mapper.SerializeUtils;
import eu.efti.eftigate.repository.RequestRepository;
import eu.efti.eftigate.service.ControlService;
import eu.efti.eftigate.service.RabbitSenderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static eu.efti.eftigate.constant.EftiGateConstants.UIL_ACTIONS;
import static eu.efti.eftigate.constant.EftiGateConstants.UIL_TYPES;

@Slf4j
@Component
public class UilRequestService extends RequestService {

    public UilRequestService(final RequestRepository requestRepository,
                             final MapperUtils mapperUtils,
                             final RabbitSenderService rabbitSenderService,
                             final ControlService controlService,
                             final GateProperties gateProperties,
                             final RequestUpdaterService requestUpdaterService,
                             final SerializeUtils serializeUtils) {
        super(requestRepository, mapperUtils, rabbitSenderService, controlService, gateProperties, requestUpdaterService, serializeUtils);
    }

    @Override
    public boolean allRequestsContainsData(final List<RequestEntity> controlEntityRequests) {
        return CollectionUtils.emptyIfNull(controlEntityRequests).stream()
                .allMatch(requestEntity -> Objects.nonNull(requestEntity.getReponseData()));
    }

    @Override
    public void setDataFromRequests(final ControlEntity controlEntity) {
        controlEntity.setEftiData(controlEntity.getRequests().stream()
                .map(RequestEntity::getReponseData).toList().stream()
                .collect(ByteArrayOutputStream::new, (byteArrayOutputStream, bytes) -> byteArrayOutputStream.write(bytes, 0, bytes.length), (arrayOutputStream, byteArrayOutputStream) -> {
                })
                .toByteArray());
    }

    @Override
    public void manageMessageReceive(final NotificationDto notificationDto) {
        final MessageBodyDto messageBody =
                getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(),MessageBodyDto.class);

        final RequestDto requestDto = this.findByRequestUuidOrThrow(messageBody.getRequestUuid());
        if (messageBody.getStatus().equals(StatusEnum.COMPLETE.name())) {
            requestDto.setReponseData(messageBody.getEFTIData().toString().getBytes(StandardCharsets.UTF_8));
            this.updateStatus(requestDto, RequestStatusEnum.SUCCESS, notificationDto);
        } else {
            this.updateStatus(requestDto, RequestStatusEnum.ERROR, notificationDto);
            errorReceived(requestDto, messageBody.getErrorDescription());
        }

        responseToOtherGateIfNecessary(requestDto);
    }

    private void responseToOtherGateIfNecessary(final RequestDto requestDto) {
        if(!requestDto.getControl().isExternalAsk()) return;
        this.updateStatus(requestDto, RequestStatusEnum.RESPONSE_IN_PROGRESS);
        requestDto.setGateUrlDest(requestDto.getControl().getFromGateUrl());
        requestDto.getControl().setEftiData(requestDto.getReponseData());
        getControlService().save(requestDto.getControl());
        this.sendRequest(requestDto);
    }

    private RequestDto findByRequestUuidOrThrow(final String requestId) {
        return getMapperUtils().requestToRequestDto(Optional.ofNullable(
                this.getRequestRepository().findByControlRequestUuidAndStatus(requestId, RequestStatusEnum.IN_PROGRESS))
                .orElseThrow(() -> new RequestNotFoundException("couldn't find request for requestUuid: " + requestId)));
    }

    @Override
    public void receiveGateRequest(final NotificationDto notificationDto) {
        final MessageBodyDto messageBody = getSerializeUtils().mapXmlStringToClass(notificationDto.getContent().getBody(), MessageBodyDto.class);

        final RequestEntity requestEntity = getRequestRepository()
                .findByControlRequestUuidAndStatus(messageBody.getRequestUuid(), RequestStatusEnum.IN_PROGRESS);

        if(requestEntity == null) {
            askReception(notificationDto, messageBody);
        } else {
            receptionOfResponse(requestEntity, messageBody);
        }
    }

    private void receptionOfResponse(final RequestEntity requestEntity, final MessageBodyDto messageBody) {
        if (!ObjectUtils.isEmpty(messageBody.getEFTIData())) {
            requestEntity.setReponseData(messageBody.getEFTIData().toString().getBytes(StandardCharsets.UTF_8));
            requestEntity.setStatus(RequestStatusEnum.SUCCESS);
        } else {
            requestEntity.setError(getMapperUtils().errorDtoToErrorEntity(ErrorDto.fromErrorCode(ErrorCodesEnum.DATA_NOT_FOUND)));
            requestEntity.setStatus(RequestStatusEnum.ERROR);
        }
        getRequestRepository().save(requestEntity);
    }

    private void askReception(final NotificationDto notificationDto, final MessageBodyDto messageBody) {
        final ControlDto controlDto = ControlDto
                .fromGateToGateMessageBodyDto(messageBody, RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH,
                        notificationDto, getGateProperties().getOwner());
        this.createAndSendRequest(getControlService().save(controlDto));
    }

    public void createAndSendRequest(final ControlDto controlDto) {
        final RequestDto requestDto = new RequestDto(controlDto);
        log.info("Request has been register with controlId : {}", requestDto.getControl().getId());
        final RequestDto result = this.save(requestDto);
        this.sendRequest(result);
    }

    @Override
    public boolean supports(final RequestTypeEnum requestTypeEnum) {
        return UIL_TYPES.contains(requestTypeEnum);
    }

    @Override
    public boolean supports(EDeliveryAction eDeliveryAction) {
        return UIL_ACTIONS.contains(eDeliveryAction);
    }
}
