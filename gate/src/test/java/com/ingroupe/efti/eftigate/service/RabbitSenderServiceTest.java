package com.ingroupe.efti.eftigate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.UilRequestDto;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.ingroupe.efti.eftigate.EftiTestUtils.testFile;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitSenderServiceTest {
    private RabbitSenderService rabbitSenderService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void before() {
        rabbitSenderService = new RabbitSenderService(rabbitTemplate);
    }

    @Test
    void sendMessageToRabbitTest() throws JsonProcessingException {
        final UilRequestDto requestDto = new UilRequestDto();
        requestDto.setStatus(RequestStatusEnum.RECEIVED);
        requestDto.setRetry(0);
        requestDto.setControl(ControlDto.builder().id(1).build());
        requestDto.setGateUrlDest("https://efti.gate.be.eu");
        requestDto.setRequestType(RequestType.UIL);

        rabbitSenderService.sendMessageToRabbit("exchange", "key", requestDto);

        //Assert
        final String requestJson = testFile("/json/request.json");

        verify(rabbitTemplate).convertAndSend("exchange", "key", StringUtils.deleteWhitespace(requestJson));
    }
}
