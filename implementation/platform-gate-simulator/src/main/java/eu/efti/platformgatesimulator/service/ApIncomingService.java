package eu.efti.platformgatesimulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.efti.platformgatesimulator.mapper.MapperUtils;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
import eu.efti.v1.json.SaveIdentifiersRequest;
import eu.efti.commons.enums.EDeliveryAction;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.dto.NotesMessageBodyDto;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.NotificationService;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.platformgatesimulator.config.GateProperties;
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

    private final Random random = new Random();

    @Autowired
    private final GateProperties gateProperties;
    private final ReaderService readerService;
    private final XmlMapper xmlMapper;
    private final MapperUtils mapperUtils = new MapperUtils();

    public void uploadIdentifiers(final SaveIdentifiersRequest identifiersDto) throws JsonProcessingException {
        eu.efti.v1.edelivery.SaveIdentifiersRequest edeliveryRequest = mapperUtils.mapToEdeliveryRequest(identifiersDto);
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(1L).body(xmlMapper.writeValueAsString(edeliveryRequest))
                .apConfig(buildApConf())
                .receiver(gateProperties.getGate())
                .sender(gateProperties.getOwner())
                .build();

        try {
            requestSendingService.sendRequest(apRequestDto, EDeliveryAction.UPLOAD_IDENTIFIERS);
        } catch (final SendRequestException e) {
            log.error("SendRequestException received : ", e);
        }
    }

    public void manageIncomingNotification(final ReceivedNotificationDto receivedNotificationDto) throws IOException, InterruptedException {
        final int rand = random.nextInt(gateProperties.getMaxSleep() - gateProperties.getMinSleep()) + gateProperties.getMinSleep();
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
            final UILQuery uilQuery = xmlMapper.readValue(notificationContentDto.getBody(), UILQuery.class);
            final String datasetId = uilQuery.getUil().getDatasetId();
            if (datasetId.endsWith("1")) {
                log.info("id {} end with 1, not responding", datasetId);
                return;
            }
            sendResponse(buildApConf(), uilQuery.getRequestId(), readerService.readFromFile(gateProperties.getCdaPath() + datasetId));
        }
    }

    private void sendResponse(final ApConfigDto apConfigDto, final String requestUuid, final SupplyChainConsignment data) throws JsonProcessingException {
        final boolean isError = data == null;
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(1L).body(buildBody(data, requestUuid, isError ? "ERROR" : "COMPLETE", isError ? "file not found with uuid" : null))
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

    private String buildBody(final SupplyChainConsignment eftiData, final String requestUuid, final String status, final String errorDescription) throws JsonProcessingException {
        final UILResponse uilResponse = new UILResponse();
        uilResponse.setRequestId(requestUuid);
        uilResponse.setDescription(errorDescription);
        uilResponse.setStatus(status);
        uilResponse.setConsignment(eftiData);

        return xmlMapper.writeValueAsString(uilResponse);
    }

    private ApConfigDto buildApConf() {
        return ApConfigDto.builder()
                .username(gateProperties.getAp().getUsername())
                .password(gateProperties.getAp().getPassword())
                .url(gateProperties.getAp().getUrl())
                .build();
    }
}

