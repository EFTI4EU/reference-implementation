package eu.efti.platformgatesimulator.service;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.v1.consignment.common.ObjectFactory;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReaderService {


    public static final String XML_FILE_TYPE = "xml";
    public static final String JSON_FILE_TYPE = "json";
    private final GateProperties gateProperties;
    private final ResourceLoader resourceLoader;

    public void uploadFile(final MultipartFile file) throws UploadException {
        try {
            if (file == null) {
                throw new NullPointerException("No file send");
            }
            log.info("Try to upload file in {} with name {}", gateProperties.getCdaPath(), file.getOriginalFilename());
            file.transferTo(new File(resourceLoader.getResource(gateProperties.getCdaPath()).getURI().getPath() + file.getOriginalFilename()));
            log.info("File uploaded in {}", gateProperties.getCdaPath() + file.getOriginalFilename());
        } catch (final IOException e) {
            log.error("Error when try to upload file to server", e);
            throw new UploadException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public SupplyChainConsignment readFromFile(final String file) throws IOException {
        Resource resource = tryOpenFile(file, XML_FILE_TYPE);
        if (!resource.exists()) {
            resource = tryOpenFile(file, JSON_FILE_TYPE);
        }
        if (resource.exists()) {
            try {
                final Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
                final JAXBElement<SupplyChainConsignment> jaxbElement = (JAXBElement<SupplyChainConsignment>) unmarshaller.unmarshal(resource.getInputStream());
                return jaxbElement.getValue();
            } catch (JAXBException e) {
                throw new TechnicalException("error while writing content", e);
            }
        }

        return null;
    }

    private Resource tryOpenFile(final String path, final String ext) {
        final String filePath = String.join(".", path, ext);
        log.info("try to open file : {}", filePath);
        return resourceLoader.getResource(filePath);
    }
}
