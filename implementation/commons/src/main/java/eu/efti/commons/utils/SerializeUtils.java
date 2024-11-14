package eu.efti.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.v1.edelivery.ObjectFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
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

    public <T, U> String mapJaxbObjectToXmlString(final T content, final Class<U> className) {
        try {
            final Marshaller marshaller = JAXBContext.newInstance(className).createMarshaller();
            final StringWriter sw = new StringWriter();
            marshaller.marshal(content, sw);
            return sw.toString();
        } catch (final JAXBException e) {
            throw new TechnicalException("error while writing content", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <U> U mapXmlStringToJaxbObject(final String content) {
        try {
            final Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
            final StringReader reader = new StringReader(content);
            final JAXBElement<U> jaxbElement = (JAXBElement<U>) unmarshaller.unmarshal(reader);
            return jaxbElement.getValue();
        } catch (final JAXBException e) {
            throw new TechnicalException("error while writing content", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <U> U mapXmlStringToJaxbObject(final String content, JAXBContext jaxbContext) {
        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final StringReader reader = new StringReader(content);
            final JAXBElement<U> jaxbElement = (JAXBElement<U>) unmarshaller.unmarshal(reader);
            return jaxbElement.getValue();
        } catch (final JAXBException e) {
            throw new TechnicalException(ERROR_WHILE_WRITING_CONTENT, e);
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
