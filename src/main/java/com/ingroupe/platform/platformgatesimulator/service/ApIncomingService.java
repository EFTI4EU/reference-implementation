package com.ingroupe.platform.platformgatesimulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ingroupe.efti.commons.dto.MetadataDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.edeliveryapconnector.dto.ApConfigDto;
import com.ingroupe.efti.edeliveryapconnector.dto.ApRequestDto;
import com.ingroupe.efti.edeliveryapconnector.dto.MessageBodyDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotesMessageBodyDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationContentDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationDto;
import com.ingroupe.efti.edeliveryapconnector.dto.NotificationType;
import com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import com.ingroupe.efti.edeliveryapconnector.exception.SendRequestException;
import com.ingroupe.efti.edeliveryapconnector.service.NotificationService;
import com.ingroupe.efti.edeliveryapconnector.service.RequestSendingService;
import com.ingroupe.platform.platformgatesimulator.config.GateProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

import static java.lang.Thread.sleep;

@Service
@AllArgsConstructor
@Slf4j
public class ApIncomingService {

    private final RequestSendingService requestSendingService;

    private final NotificationService notificationService;

    @Autowired
    private final GateProperties gateProperties;
    private final ReaderService readerService;
    private final XmlMapper xmlMapper;

    public void uploadMetadata(final MetadataDto metadataDto) throws JsonProcessingException {
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(1L).body(xmlMapper.writeValueAsString(metadataDto))
                .apConfig(buildApConf())
                .receiver(gateProperties.getGate())
                .sender(gateProperties.getOwner())
                .build();

        try {
            requestSendingService.sendRequest(apRequestDto, EDeliveryAction.UPLOAD_METADATA);
        } catch (final SendRequestException e) {
            log.error("SendRequestException received : ", e);
        }
    }

    public void manageIncomingNotification(final ReceivedNotificationDto receivedNotificationDto) throws IOException, InterruptedException {
        final int rand = new Random().nextInt(gateProperties.getMaxSleep()-gateProperties.getMinSleep())+gateProperties.getMinSleep();
        sleep(rand);

        final Optional<NotificationDto> notificationDto = notificationService.consume(receivedNotificationDto);
        if (notificationDto.isEmpty() || notificationDto.get().getNotificationType() == NotificationType.SEND_SUCCESS
                || notificationDto.get().getNotificationType() == NotificationType.SEND_FAILURE) {
            return;
        }
        final NotificationContentDto notificationContentDto = notificationDto.get().getContent();
        final EDeliveryAction action = EDeliveryAction.getFromValue(notificationContentDto.getAction());

        if (action == EDeliveryAction.SEND_NOTES) {
            final NotesMessageBodyDto messageBody = xmlMapper.readValue(notificationContentDto.getBody(), NotesMessageBodyDto.class);
            log.info("note \"{}\" received for request with id {}", messageBody.getNote(), messageBody.getRequestUuid());
        } else {
            final MessageBodyDto messageBody = xmlMapper.readValue(notificationContentDto.getBody(), MessageBodyDto.class);
            final String eftidataUuid = messageBody.getEFTIDataUuid();
            if (eftidataUuid.endsWith("1")) {
                return;
            }
            sendResponse(buildApConf(), eftidataUuid, messageBody.getRequestUuid(), readerService.readFromFile(gateProperties.getCdaPath() + eftidataUuid));
        }
    }

    private void sendResponse(final ApConfigDto apConfigDto, final String eftidataUuid, final String requestUuid, final String data) throws JsonProcessingException {
        final boolean isError = data == null;
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(1L).body(buildBody(data, requestUuid, eftidataUuid, isError? "ERROR" : "COMPLETE", isError ? "file not found with uuid" : null))
                .apConfig(apConfigDto)
                .receiver(gateProperties.getGate())
                .sender(gateProperties.getOwner())
                .build();
        try {
            requestSendingService.sendRequest(apRequestDto, EDeliveryAction.GET_UIL);
        } catch (final SendRequestException e) {
            log.error("SendRequestException received : ", e);
        }
    }

    private String buildBody(final String eftiData, final String requestUuid, final String eftidataUuid, final String status, final String errorDescription) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(MessageBodyDto.builder()
                .requestUuid(requestUuid)
                .eFTIData(eftiData)
                .status(status)
                .errorDescription(errorDescription)
                .eFTIDataUuid(eftidataUuid)
                .build());
    }

    private ApConfigDto buildApConf() {
        return ApConfigDto.builder()
                .username(gateProperties.getAp().getUsername())
                .password(gateProperties.getAp().getPassword())
                .url(gateProperties.getAp().getUrl())
                .build();
    }
}

