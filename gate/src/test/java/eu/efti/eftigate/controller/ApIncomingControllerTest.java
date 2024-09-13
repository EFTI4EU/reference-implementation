package eu.efti.eftigate.controller;

import eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto;
import eu.efti.eftigate.service.RabbitSenderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.MESSAGE_ID;
import static eu.efti.edeliveryapconnector.dto.ReceivedNotificationDto.SENT_SUCCESS;

@WebMvcTest(ApIncomingController.class)
@ContextConfiguration(classes= {ApIncomingController.class})
@ExtendWith(SpringExtension.class)
class ApIncomingControllerTest {

    @MockBean
    private ApIncomingController apIncomingController;

    @Autowired
    protected MockMvc mockMvc;

    @Mock
    private RabbitSenderService rabbitSenderService;

    @BeforeEach
    public void before() {
        apIncomingController = new ApIncomingController(rabbitSenderService);
        ReflectionTestUtils.setField(apIncomingController, "eftiReceiveMessageExchange", "eftiReceiveMessageExchange");
        ReflectionTestUtils.setField(apIncomingController, "eftiKeySendMessage", "eftiKeySendMessage");
    }

    @Test
    void getByIdTestWithData() {
        final Map<String, Map<String, Object>> body = Map.of(SENT_SUCCESS, Map.of(MESSAGE_ID, "test"));

        final ReceivedNotificationDto receivedNotificationDto = new ReceivedNotificationDto();
        receivedNotificationDto.setBody(body);

        final ResponseEntity<String> result = apIncomingController.incoming(receivedNotificationDto);

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
