package eu.efti.platformgatesimulator.service;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.utils.SubsetUtils;
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
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReaderService {
    public static final String XML_FILE_TYPE = "xml";
    public static final String JSON_FILE_TYPE = "json";
    private final GateProperties gateProperties;
    private final ResourceLoader resourceLoader;

    public void uploadFile(final MultipartFile file) throws UploadException {
        uploadFile(file, file.getOriginalFilename());
    }

    public void uploadFile(final MultipartFile file, final String filenameOverride) throws UploadException {
        try {
            if (file == null) {
                throw new NullPointerException("No file send");
            }
            log.info("Try to upload file in {} with name {}", gateProperties.getCdaPath(), filenameOverride);
            file.transferTo(new File(resourceLoader.getResource(gateProperties.getCdaPath()).getURI().getPath() + filenameOverride));
            log.info("File uploaded in {}", gateProperties.getCdaPath() + filenameOverride);
        } catch (final IOException e) {
            log.error("Error when try to upload file to server", e);
            throw new UploadException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public SupplyChainConsignment readFromFile(final String file, final List<String> subsets) throws IOException {
        Resource resource = tryOpenFile(file, XML_FILE_TYPE);
        if (resource.exists()) {
            Optional<String> str;
            if (subsets.isEmpty() || subsets.contains("full")) {
                str = Optional.of(resource.getContentAsString(Charset.defaultCharset()));
            } else {
                str = SubsetUtils.parseBySubsets(resource.getContentAsString(Charset.defaultCharset()), subsets);
            }
            if (str.isEmpty()) {
                return null;
            }
            try {
                final Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
                final JAXBElement<SupplyChainConsignment> jaxbElement = (JAXBElement<SupplyChainConsignment>) unmarshaller.unmarshal(new InputSource(new StringReader(str.get())));
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
