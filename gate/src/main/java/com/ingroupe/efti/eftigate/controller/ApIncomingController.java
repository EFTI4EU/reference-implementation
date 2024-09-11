package com.ingroupe.efti.eftigate.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ingroupe.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import com.ingroupe.efti.eftigate.service.RabbitSenderService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
@Slf4j
public class ApIncomingController {

    private static final String SOAP_RESULT = """
            <Envelope xmlns="http://schemas.xmlsoap.org/soap/envelope/">
               <Body> domibus ws plugin require a response when it call our endpoint </Body>
            </Envelope>
           """;

    private final RabbitSenderService rabbitSenderService;

    @Value("${spring.rabbitmq.queues.eftiReceiveMessageExchange:efti.send-message.exchange}")
    private String eftiReceiveMessageExchange;

    @Value("${spring.rabbitmq.queues.eftiKeySendMessage:EFTI}")
    private String eftiKeySendMessage;

    @PostMapping("/notification")
    public ResponseEntity<String> incoming(final @RequestBody ReceivedNotificationDto receivedNotificationDto) {
        log.info("receive notification from domibus");
        try {
            rabbitSenderService.sendMessageToRabbit(eftiReceiveMessageExchange, eftiKeySendMessage, receivedNotificationDto);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to parse message and send it to the rabbitmq", e);
        }
        return ResponseEntity.ok().body(SOAP_RESULT);
    }
}
