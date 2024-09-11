package com.ingroupe.efti.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ingroupe.efti.commons.exception.TechnicalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class SerializeUtils {

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    public <T> T mapJsonStringToClass(final String message, final Class<T> className) {
        try {
            final JavaType javaType = objectMapper.getTypeFactory().constructType(className);
            return objectMapper.readValue(message, javaType);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to parse message to " + className, e);
            throw new TechnicalException("Error when try to map " + className + " with message : " + message);
        }
    }

    public <T> T mapXmlStringToClass(final String message, final Class<T> className) {
        try {
            final JavaType javaType = xmlMapper.getTypeFactory().constructType(className);
            return xmlMapper.readValue(message, javaType);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to parse message to " + className, e);
            throw new TechnicalException("Error when try to map " + className + " with message : " + message);
        }
    }

    public <T> String mapObjectToXmlString(final T content) {
        try {
            return xmlMapper.writeValueAsString(content);
        } catch (final JsonProcessingException e) {
            throw new TechnicalException("error while writing content", e);
        }
    }

    public <T> String mapObjectToJsonString(final T content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (final JsonProcessingException e) {
            throw new TechnicalException("error while writing content", e);
        }
    }

    public <T> String mapObjectToBase64String(final T content) {
        return new String(Base64.getEncoder().encode(this.mapObjectToJsonString(content).getBytes()), StandardCharsets.UTF_8);
    }
}
