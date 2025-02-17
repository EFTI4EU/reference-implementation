package eu.efti.eftigate.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ValidationService {

    Validator validator;

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    @Value("${gate.xsd.path:classpath:xsd/edelivery/gate.xsd}")
    private String gateXsd;

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
        return ResourceUtils.getFile(gateXsd);
    }

    private Validator initValidator() throws FileNotFoundException, SAXException {
        Source schemaFile = new StreamSource(getFile());

        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public void validateXml(String xml) throws SAXException, IOException {
        List<SAXParseException> exceptions = new ArrayList<>();
        validator.setErrorHandler(new ErrorHandler() {

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                log.warn(exception.getMessage());
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                log.error(exception.getMessage());
                exceptions.add(exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                log.error(exception.getMessage());
                exceptions.add(exception);
            }
        });
        validator.validate(new StreamSource(new StringReader(xml)));
        if (CollectionUtils.isNotEmpty(exceptions)) {
            throw exceptions.get(0);
        }
    }
}
