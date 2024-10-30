package eu.efti.platformgatesimulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.constant.EDeliveryStatus;
import eu.efti.platformgatesimulator.mapper.MapperUtils;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.ObjectFactory;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
import eu.efti.v1.json.SaveIdentifiersRequest;
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
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static java.lang.Thread.sleep;

@Service
@AllArgsConstructor
@Slf4j
public class ApIncomingService {
    private static final String NOT_FOUND_MESSAGE = "file not found with uuid";

    private final RequestSendingService requestSendingService;

    private final NotificationService notificationService;

    private final Random random = new Random();

    private final GateProperties gateProperties;
    private final ReaderService readerService;
    private final MapperUtils mapperUtils = new MapperUtils();
    private final SerializeUtils serializeUtils;
    private final ObjectFactory objectFactory = new ObjectFactory();

    public void uploadIdentifiers(final SaveIdentifiersRequest identifiersDto) throws JsonProcessingException {
        final eu.efti.v1.edelivery.SaveIdentifiersRequest edeliveryRequest = mapperUtils.mapToEdeliveryRequest(identifiersDto);
        final JAXBElement<eu.efti.v1.edelivery.SaveIdentifiersRequest> jaxbElement = objectFactory.createSaveIdentifiersRequest(edeliveryRequest);
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(UUID.randomUUID().toString()).body(serializeUtils.mapJaxbObjectToXmlString(jaxbElement, eu.efti.v1.edelivery.SaveIdentifiersRequest.class))
                .apConfig(buildApConf())
                .receiver(gateProperties.getGate())
                .sender(gateProperties.getOwner())
                .build();

        try {
            requestSendingService.sendRequest(apRequestDto);
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

        final XmlType queryAnnotation = UILQuery.class.getAnnotation((XmlType.class));
        if (StringUtils.containsIgnoreCase(notificationContentDto.getBody(), "<" + queryAnnotation.name())) {
            final UILQuery uilQuery = serializeUtils.mapXmlStringToJaxbObject(notificationContentDto.getBody());
            final String datasetId = uilQuery.getUil().getDatasetId();
            if (datasetId.endsWith("1")) {
                log.info("id {} end with 1, not responding", datasetId);
                return;
            }
            sendResponse(buildApConf(), uilQuery.getRequestId(), readerService.readFromFile(gateProperties.getCdaPath() + datasetId));
        } else {
            final NotesMessageBodyDto messageBody = serializeUtils.mapXmlStringToClass(notificationContentDto.getBody(), NotesMessageBodyDto.class);
            log.info("note \"{}\" received for request with id {}", messageBody.getNote(), messageBody.getRequestId());
        }
    }

    private void sendResponse(final ApConfigDto apConfigDto, final String requestId, final SupplyChainConsignment data) {
        final boolean notFound = data == null;
        final ApRequestDto apRequestDto = ApRequestDto.builder()
                .requestId(requestId).body(buildBody(data, requestId, notFound))
                .apConfig(apConfigDto)
                .receiver(gateProperties.getGate())
                .sender(gateProperties.getOwner())
                .build();
        try {
            requestSendingService.sendRequest(apRequestDto);
        } catch (final SendRequestException e) {
            log.error("SendRequestException received : ", e);
        }
    }

    private String buildBody(final SupplyChainConsignment data, final String requestId, final boolean notFound) {
        final UILResponse uilResponse = new UILResponse();
        uilResponse.setRequestId(requestId);
        uilResponse.setDescription(notFound ? NOT_FOUND_MESSAGE :  null);
        uilResponse.setStatus(notFound ? EDeliveryStatus.NOT_FOUND.getCode() : EDeliveryStatus.OK.getCode());
        uilResponse.setConsignment(data);

        final JAXBElement<UILResponse> jaxbElement = objectFactory.createUilResponse(uilResponse);
        return serializeUtils.mapJaxbObjectToXmlString(jaxbElement, UILResponse.class);
    }

    private ApConfigDto buildApConf() {
        return ApConfigDto.builder()
                .username(gateProperties.getAp().getUsername())
                .password(gateProperties.getAp().getPassword())
                .url(gateProperties.getAp().getUrl())
                .build();
    }
}

