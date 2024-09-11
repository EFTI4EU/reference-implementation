package com.ingroupe.efti.eftigate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitSenderService {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessageToRabbit(final String exchange, final String key, final Object message) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final ObjectWriter ow = objectMapper.writer();
        final String json = ow.writeValueAsString(message);
        rabbitTemplate.convertAndSend(exchange, key, json);
    }
}
