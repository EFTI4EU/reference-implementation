package eu.efti.platformgatesimulator.controller;

import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/ws")
@AllArgsConstructor
@Slf4j
public class ApIncomingController {

    private final ApIncomingService apIncomingService;

    @PostMapping("/notification")
    public ResponseEntity<String> getById(final @RequestBody ReceivedNotificationDto receivedNotificationDto) throws IOException, InterruptedException {
        log.info("Notification reçus");

        apIncomingService.manageIncomingNotification(receivedNotificationDto);
        String soapResult = """
                 <Envelope xmlns="http://schemas.xmlsoap.org/soap/envelope/">
                    <Body> domibus ws plugin require a response when it call our endpoint </Body>
                 </Envelope>
                """;
        return ResponseEntity.ok().body(soapResult);
    }
}
