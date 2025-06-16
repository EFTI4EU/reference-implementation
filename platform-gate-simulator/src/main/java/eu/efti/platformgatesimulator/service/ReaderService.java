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
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReaderService {
    public static final String XML_FILE_TYPE = "xml";
    public static final String XML_ENDING = ".xml";
    public static final char CHAR_DOT = '.';
    public static final String EMPTY_STRING = "";
    private final GateProperties gateProperties;

    public void uploadFile(final MultipartFile file) throws UploadException {
        uploadFile(file, file.getOriginalFilename());
    }

    public void uploadFile(final MultipartFile file, final String filenameOverride) throws UploadException {
        try {
            if (file == null) {
                throw new IllegalArgumentException("No file provided or file is empty");
            }
            String targetDirectory = gateProperties.getCdaPath();
            log.info("Try to upload file in {} with name {}", targetDirectory, filenameOverride);
            file.transferTo(new File(targetDirectory + File.separator + filenameOverride).toPath());
            log.info("File uploaded in {}", targetDirectory + filenameOverride);
        } catch (IOException ex) {
            log.error("Error when try to upload file to server", ex);
            throw new UploadException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public SupplyChainConsignment readFromFile(final String file, final List<String> subsets) throws IOException, IllegalArgumentException {
        final String content;
        try {
            content = tryOpenFileOrThrow(file);
        } catch (FileReaderException e) {
            log.error("Error when try to open file", e);
            return null;
        }

        try {
            final String contentFiltered = filterBySubset(content, subsets);
            final Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
            final JAXBElement<SupplyChainConsignment> jaxbElement = (JAXBElement<SupplyChainConsignment>) unmarshaller.unmarshal(new InputSource(new StringReader(contentFiltered)));
            return jaxbElement.getValue();
        } catch (JAXBException e) {
            throw new TechnicalException("error while writing content", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
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
            try {
                return SubsetUtils.parseBySubsets(content, subsets).orElse(null);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage());
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    private void checkDestinationFolder() throws UploadException {
        final File folderDest = new File(gateProperties.getCdaPath());
        if (!folderDest.exists() && !folderDest.mkdirs()) {
            throw new UploadException("destination folder could not be created");
        }
    }

    private String checkStringXml(final String fileName) {
        int lenghtString = fileName.length();
        final String xmlName = fileName.substring(lenghtString - 4, lenghtString);
        if (CHAR_DOT == fileName.charAt(0)) {
            return EMPTY_STRING;
        }
        if (XML_ENDING.equals(xmlName)) {
            return fileName.substring(0, lenghtString - 4);
        }
        return fileName;
    }

    public boolean deleteFile(final String fileName) {
        log.info("Try to delete file: {}", fileName);
        try {
            checkDestinationFolder();
        } catch (Exception e) {
            log.error("Error path destination is not good", e);
            return false;
        }
        final String finalName = checkStringXml(fileName);
        final File file = new File(gateProperties.getCdaPath() + File.separator + finalName + XML_ENDING);
        return deleteFile(file);
    }

    public boolean deleteAllFile() {
        try {
            checkDestinationFolder();
            final File folderDest = new File(gateProperties.getCdaPath());
            List<File> files = List.of(Objects.requireNonNull(folderDest.listFiles()));
            files.forEach(this::deleteFile);
        } catch (Exception e) {
            log.error("Error when try to delete all file", e);
            return false;
        }
        return true;
    }

    private boolean deleteFile(File file) {
        log.info("try to delete: {}", file.getName());
        boolean result = false;
        try {
            result = Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.error("Error can't delete file {}: ", file.getName(), e);
        }
        log.info(result ? "file {} deleted" : "Error when try to delete file: {}", file.getName());
        log.info("-------------------------------------------------------------------");
        return result;
    }
}
