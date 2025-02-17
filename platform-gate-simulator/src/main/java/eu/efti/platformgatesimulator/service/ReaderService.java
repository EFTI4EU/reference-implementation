package eu.efti.platformgatesimulator.service;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.exception.FileReaderException;
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
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReaderService {
    public static final String XML_FILE_TYPE = "xml";
    private final GateProperties gateProperties;

    public void uploadFile(final MultipartFile file) throws UploadException {
        checkDestinationFolder();

        final String path = gateProperties.getCdaPath() + File.separator + file.getOriginalFilename();
        log.info("Try to upload file in {} with name {}", gateProperties.getCdaPath(), file.getOriginalFilename());

        try {
            Files.copy(file.getInputStream(), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Error when try to upload file to server", ex);
            throw new UploadException(ex);
        }
        log.info("File uploaded in {}", gateProperties.getCdaPath() + file.getOriginalFilename());
    }

    @SuppressWarnings("unchecked")
    public SupplyChainConsignment readFromFile(final String file, final List<String> subsets) throws IOException {
        final String content;
        try {
            content = tryOpenFileOrThrow(file);
        } catch (FileReaderException e) {
            log.error("Error when try to open file", e);
            return null;
        }

        final String contentFiltered = filterBySubset(content, subsets);

        try {
            final Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
            final JAXBElement<SupplyChainConsignment> jaxbElement = (JAXBElement<SupplyChainConsignment>) unmarshaller.unmarshal(new InputSource(new StringReader(contentFiltered)));
            return jaxbElement.getValue();
        } catch (JAXBException e) {
            throw new TechnicalException("error while writing content", e);
        }
    }

    private String tryOpenFileOrThrow(final String path) throws FileReaderException {
        log.info("try to open file : {}", path);
        final String filePath = String.join(".", path, XML_FILE_TYPE);
        final File file = new File(filePath);
        try (final BOMInputStream bomInputStream = new BOMInputStream(new FileInputStream(file))) {
            return new String(bomInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileReaderException(e);
        }
    }

    private String filterBySubset(final String content, final List<String> subsets) {
        if (subsets.isEmpty() || subsets.contains("full")) {
            return content;
        } else {
            return SubsetUtils.parseBySubsets(content, subsets).orElse(null);
        }
    }

    private void checkDestinationFolder() throws UploadException {
        final File folderDest = new File(gateProperties.getCdaPath());
        if(!folderDest.exists() && !folderDest.mkdirs()) {
            throw new UploadException("destination folder could not be created");
        }
    }
}
