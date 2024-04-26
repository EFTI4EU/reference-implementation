package com.ingroupe.efti.eftigate.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ingroupe.efti.edeliveryapconnector.exception.RetrieveMessageException;
import com.ingroupe.efti.eftigate.exception.TechnicalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import javax.activation.DataSource;
import java.io.IOException;

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

    public <T> String mapObjectToJsonString(final T content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (final JsonProcessingException e) {
            throw new TechnicalException("error while writing content", e);
        }
    }

    public String readDataSourceOrThrow(final DataSource dataSource) {
        try {
            return IOUtils.toString(dataSource.getInputStream());
        } catch (final IOException e) {
            throw new RetrieveMessageException("error while managing message received", e);
        }
    }

    public <T> T mapDataSourceToClass(final DataSource dataSource, final Class<T> className) {
        return this.mapJsonStringToClass(readDataSourceOrThrow(dataSource), className);
    }
}
