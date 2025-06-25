package eu.efti.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.v1.edelivery.ObjectFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
@Slf4j
public class SerializeUtils {

    public static final String ERROR_WHILE_WRITING_CONTENT = "error while writing content";
    private final ObjectMapper objectMapper;
    private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    public String mapDocToXmlString(Document doc) {
        return mapDocToXmlString(doc, false);
    }

    public String mapDocToXmlString(Document doc, boolean prettyPrint) {
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS domImplLS = (DOMImplementationLS) registry.getDOMImplementation("LS");

            LSSerializer lsSerializer = domImplLS.createLSSerializer();
            DOMConfiguration domConfig = lsSerializer.getDomConfig();
            domConfig.setParameter("format-pretty-print", prettyPrint);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            LSOutput lsOutput = domImplLS.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            lsOutput.setByteStream(byteArrayOutputStream);

            lsSerializer.write(doc, lsOutput);
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new TechnicalException("Could not serialize", e);
        }
    }

    public <T> T mapJsonStringToClass(final String message, final Class<T> className) {
        try {
            final JavaType javaType = objectMapper.getTypeFactory().constructType(className);
            return objectMapper.readValue(message, javaType);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to parse message to " + className, e);
            throw new TechnicalException("Error when try to map " + className + " with message : " + message);
        }
    }

    public <T, U> String mapJaxbObjectToXmlString(final T content, final Class<U> className) {
        try {
            final Marshaller marshaller = JAXBContext.newInstance(className).createMarshaller();
            final StringWriter sw = new StringWriter();
            marshaller.marshal(content, sw);
            return sw.toString();
        } catch (final JAXBException e) {
            throw new TechnicalException(ERROR_WHILE_WRITING_CONTENT, e);
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
            throw new TechnicalException(ERROR_WHILE_WRITING_CONTENT, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <U> U mapXmlStringToJaxbObject(final String content, final JAXBContext jaxbContext) {
        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final StringReader reader = new StringReader(content);
            final JAXBElement<U> jaxbElement = (JAXBElement<U>) unmarshaller.unmarshal(reader);
            return jaxbElement.getValue();
        } catch (final JAXBException e) {
            throw new TechnicalException(ERROR_WHILE_WRITING_CONTENT, e);
        }
    }

    public <U> U mapXmlStringToJaxbObject(final String content, Class<U> clazz) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final StreamSource source = new StreamSource(new ByteArrayInputStream(content.getBytes()));
            final JAXBElement<U> jaxbElement = unmarshaller.unmarshal(source, clazz);
            return jaxbElement.getValue();
        } catch (final JAXBException e) {
            throw new TechnicalException("Could not unmarshal", e);
        }
    }

    public <T> String mapObjectToJsonString(final T content) {
        try {
            objectMapper.registerModule(new Jdk8Module());
            return objectMapper.writeValueAsString(content);
        } catch (final JsonProcessingException e) {
            throw new TechnicalException(ERROR_WHILE_WRITING_CONTENT, e);
        }
    }

    public <T> String mapObjectToBase64String(final T content) {
        return new String(Base64.getEncoder().encode(this.mapObjectToJsonString(content).getBytes(UTF_8)), UTF_8);
    }

    public static <U> Document mapJaxbObjectToDoc(U object, Class<U> clazz, String rootName, String rootNamespace) {
        try {
            Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
            JAXBContext.newInstance(clazz).createMarshaller().marshal(new JAXBElement<>(
                            new QName(rootNamespace, rootName),
                            clazz,
                            null,
                            object
                    ),
                    doc);

            return doc;
        } catch (Exception e) {
            throw new TechnicalException("Could not serialize object", e);
        }
    }

    public <U> U mapXmlStringToJaxbObject(final String content, Class<U> clazz, Schema schema) {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            // Set schema to enable validation
            unmarshaller.setSchema(schema);
            final StreamSource source = new StreamSource(new ByteArrayInputStream(content.getBytes()));
            final JAXBElement<U> jaxbElement = unmarshaller.unmarshal(source, clazz);
            return jaxbElement.getValue();
        } catch (final ValidationException | UnmarshalException e) {
            throw new TechnicalException("Mapping Error", e);
        } catch (final JAXBException e) {
            throw new TechnicalException("Could not unmarshal", e);
        }
    }
}
