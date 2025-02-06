package eu.efti.eftigate.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

@Service
@Slf4j
public class ValidationService {

    Validator validator;

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    @Value("${gate.xsd.path:classpath:xsd/edelivery/gate.xsd}")
    private String GATE_XSD;

    @PostConstruct
    public void postConstruct() {
        try {
            validator = initValidator();
        } catch (FileNotFoundException | SAXException e) {
            log.error("can't initialize ValidationService", e);
            throw new IllegalArgumentException();
        }
    }

    private File getFile() throws FileNotFoundException {
        return ResourceUtils.getFile(GATE_XSD);
    }

    private Validator initValidator() throws FileNotFoundException, SAXException {
        Source schemaFile = new StreamSource(getFile());

        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public Optional<String> isXmlValid(final String body) {
        try {
            validator.validate(new StreamSource(new StringReader(body)));
        } catch (SAXException | IOException e) {
            log.error("Error with XSD", e);
            return Optional.of(e.getMessage());
        }
        return Optional.empty();
    }

}
