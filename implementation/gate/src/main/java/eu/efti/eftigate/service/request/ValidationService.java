package eu.efti.eftigate.service.request;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.Optional;

@Service
@Slf4j
public class ValidationService {

    private static final String GATE_XSD = "xsd/edelivery/gate.xsd";

    @Getter
    private Schema gateSchema;

    Validator validator;

    public ValidationService() {
        try {
            validator = initValidator();
        } catch (SAXException e) {
            log.error("can't initialize ValidationService", e);
            throw new IllegalArgumentException(e);
        }
    }

    private Validator initValidator() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver((type, namespaceURI, publicId, systemId, baseURI) -> {
            if (systemId != null) {
                String path = systemId;
                if (systemId.contains("/")) {
                    path = systemId.substring(systemId.lastIndexOf('/') + 1);
                }

                // Try different possible locations from inside the JAR
                for (String prefix : new String[]{"xsd/", "xsd/types/", "xsd/codes/", "xsd/edelivery/"}) {
                    InputStream stream = getClass().getClassLoader().getResourceAsStream(prefix + path);
                    if (stream != null) {
                        LSInput input = new XSDInsideJarInput();
                        input.setPublicId(publicId);
                        input.setSystemId(systemId);
                        input.setBaseURI(baseURI);
                        input.setByteStream(stream);
                        input.setEncoding("UTF-8");
                        return input;
                    }
                }
            }
            return null;
        });
        Source schemaFile = new StreamSource(getClass().getClassLoader().getResourceAsStream(GATE_XSD));
        gateSchema = factory.newSchema(schemaFile);
        return gateSchema.newValidator();
    }

    public Optional<String> isXmlValid(String body) {
        try {
            validator.validate(new StreamSource(new StringReader(body)));
        } catch (SAXException | IOException e) {
            log.error("Error with XSD", e);
            return Optional.of(e.getMessage());
        }
        return Optional.empty();
    }

    private static class XSDInsideJarInput implements LSInput {

        private Reader characterStream;
        private InputStream byteStream;
        private String stringData;
        private String systemId;
        private String publicId;
        private String baseURI;
        private String encoding;
        private boolean certifiedText;

        @Override
        public Reader getCharacterStream() {
            return characterStream;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
            this.characterStream = characterStream;
        }

        @Override
        public InputStream getByteStream() {
            return byteStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
            this.byteStream = byteStream;
        }

        @Override
        public String getStringData() {
            return stringData;
        }

        @Override
        public void setStringData(String stringData) {
            this.stringData = stringData;
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        @Override
        public String getBaseURI() {
            return baseURI;
        }

        @Override
        public void setBaseURI(String baseURI) {
            this.baseURI = baseURI;
        }

        @Override
        public String getEncoding() {
            return encoding;
        }

        @Override
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public boolean getCertifiedText() {
            return certifiedText;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
            this.certifiedText = certifiedText;
        }
    }

}
