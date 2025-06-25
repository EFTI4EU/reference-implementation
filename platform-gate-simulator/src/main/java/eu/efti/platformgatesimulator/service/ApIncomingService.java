package eu.efti.platformgatesimulator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.edeliveryapconnector.dto.ApConfigDto;
import eu.efti.edeliveryapconnector.dto.ApRequestDto;
import eu.efti.edeliveryapconnector.dto.NotificationContentDto;
import eu.efti.edeliveryapconnector.dto.NotificationDto;
import eu.efti.edeliveryapconnector.dto.NotificationType;
import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.edeliveryapconnector.exception.SendRequestException;
import eu.efti.edeliveryapconnector.service.NotificationService;
import eu.efti.edeliveryapconnector.service.RequestSendingService;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.mapper.MapperUtils;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import eu.efti.v1.edelivery.IdentifierQuery;
import eu.efti.v1.edelivery.IdentifierResponse;
import eu.efti.v1.edelivery.ObjectFactory;
import eu.efti.v1.edelivery.PostFollowUpRequest;
import eu.efti.v1.edelivery.UILQuery;
import eu.efti.v1.edelivery.UILResponse;
import eu.efti.v1.json.SaveIdentifiersRequest;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ApIncomingService {

    private final RequestSendingService requestSendingService;

    private final NotificationService notificationService;

    private final GateProperties gateProperties;
    private final MapperUtils mapperUtils = new MapperUtils();
    private final SerializeUtils serializeUtils;
    private final ObjectFactory objectFactory = new ObjectFactory();

    private final ReaderService readerService;

    private final IdentifierService identifierService;


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

    public void manageIncomingNotification(final ReceivedNotificationDto receivedNotificationDto) {
        final Optional<NotificationDto> notificationDto = notificationService.consume(receivedNotificationDto);
        if (notificationDto.isEmpty() || notificationDto.get().getNotificationType() == NotificationType.SEND_SUCCESS
                || notificationDto.get().getNotificationType() == NotificationType.SEND_FAILURE) {
            return;
        }

        final NotificationContentDto notificationContentDto = notificationDto.get().getContent();

        final XmlType queryAnnotationUilResponse = UILResponse.class.getAnnotation((XmlType.class));
        if (StringUtils.containsIgnoreCase(notificationContentDto.getBody(), "<" + queryAnnotationUilResponse.name())) {
            log.info("Receive UilResponse");
            return;
        }
        final XmlType responseIdentifierAnnotation = IdentifierResponse.class.getAnnotation((XmlType.class));
        if (StringUtils.containsIgnoreCase(notificationContentDto.getBody(), "<" + responseIdentifierAnnotation.name())) {
            log.info("Receive IdentifierResponse");
            return;
        }

        final XmlType queryAnnotationIdentifierQuery = IdentifierQuery.class.getAnnotation((XmlType.class));
        if (StringUtils.containsIgnoreCase(notificationContentDto.getBody(), "<" + queryAnnotationIdentifierQuery.name())) {
            final IdentifierQuery identifierQuery = serializeUtils.mapXmlStringToJaxbObject(notificationContentDto.getBody());
            identifierService.sendResponseIdentifier(identifierQuery, notificationDto.get());
            return;
        }

        final XmlType queryAnnotation = UILQuery.class.getAnnotation((XmlType.class));
        if (StringUtils.containsIgnoreCase(notificationContentDto.getBody(), "<" + queryAnnotation.name())) {
            final UILQuery uilQuery = serializeUtils.mapXmlStringToJaxbObject(notificationContentDto.getBody());
            final String datasetId = uilQuery.getUil().getDatasetId();
            if (datasetId.endsWith("1")) {
                log.info("id {} end with 1, not responding", datasetId);
                return;
            }
            try {
                final SupplyChainConsignment supplyChainConsignment = readerService.readFromFile(gateProperties.getCdaPath() + File.separator + datasetId, uilQuery.getSubsetId());
                identifierService.sendResponseUil(uilQuery.getRequestId(), supplyChainConsignment);
            } catch (IOException e) {
                log.error("Error can't read file");
            } catch (IllegalArgumentException e) {
                identifierService.sendBadResponseIllegalArgument(uilQuery.getRequestId(), e.getMessage());
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            final PostFollowUpRequest messageBody = serializeUtils.mapXmlStringToJaxbObject(notificationDto.get().getContent().getBody());
            log.info("note \"{}\" received for request with id {}", messageBody.getMessage(), messageBody.getRequestId());
        }
    }

    private ApConfigDto buildApConf() {
        return ApConfigDto.builder()
                .username(gateProperties.getAp().getUsername())
                .password(gateProperties.getAp().getPassword())
                .url(gateProperties.getAp().getUrl())
                .build();
    }
}

