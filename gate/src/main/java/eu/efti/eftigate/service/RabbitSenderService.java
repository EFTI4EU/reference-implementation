package eu.efti.eftigate.service;

import eu.efti.commons.utils.SerializeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitSenderService {

    private final RabbitTemplate rabbitTemplate;

    private final SerializeUtils serializeUtils;

    public void sendMessageToRabbit(final String exchange, final String key, final Object message) {
        final String json = serializeUtils.mapObjectToJsonString(message);
        rabbitTemplate.convertAndSend(exchange, key, json);
    }
}
