package eu.efti.eftigate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.ControlDto;
import eu.efti.commons.dto.UilRequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.utils.SerializeUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static eu.efti.eftigate.EftiTestUtils.testFile;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitSenderServiceTest extends AbstractServiceTest{
    private RabbitSenderService rabbitSenderService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void before() {
        rabbitSenderService = new RabbitSenderService(rabbitTemplate, serializeUtils);
    }

    @Test
    void sendMessageToRabbitTest() throws JsonProcessingException {
        final UilRequestDto requestDto = new UilRequestDto();
        requestDto.setStatus(RequestStatusEnum.RECEIVED);
        requestDto.setRetry(0);
        requestDto.setControl(ControlDto.builder().id(1).build());
        requestDto.setGateIdDest("https://efti.gate.be.eu");
        requestDto.setRequestType(RequestType.UIL);

        rabbitSenderService.sendMessageToRabbit("exchange", "key", requestDto);

        //Assert
        final String requestJson = testFile("/json/request.json");

        verify(rabbitTemplate).convertAndSend("exchange", "key", StringUtils.deleteWhitespace(requestJson));
    }
}
