package eu.efti.eftigate.service;

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


    @Value("${gate.xsd.path:classpath:xsd/edelivery/gate.xsd}")
    private String gateXsd;

    private File getFile() throws FileNotFoundException {
        File file = ResourceUtils.getFile(gateXsd);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath() + " not found");
        }
        return file;
    }

    public void validateXml(final String xml) throws SAXException, IOException {
        File xsd = getFile();
        log.debug("Validating XML with xsd {}", xsd.getAbsolutePath());
        final Source schemaFile = new StreamSource(xsd);
        final Source xmlFile = new StreamSource(new StringReader(xml));
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);//NOSONAR
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        List<SAXParseException> exceptions = new ArrayList<>();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(final SAXParseException exception) {
                log.warn(exception.getMessage());
            }

            @Override
            public void error(final SAXParseException exception) {
                log.error(exception.getMessage());
                exceptions.add(exception);
            }

            @Override
            public void fatalError(final SAXParseException exception) {//NOSONAR
                log.error(exception.getMessage());
                exceptions.add(exception);
            }
        });
        validator.validate(xmlFile);
        if (CollectionUtils.isNotEmpty(exceptions)) {
            log.debug("Validation exceptions found: {}", exceptions.size());
            throw exceptions.get(0);
        } else {
            log.debug("Validation successful");
        }
    }
}
