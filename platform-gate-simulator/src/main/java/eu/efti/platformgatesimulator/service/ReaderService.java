package eu.efti.platformgatesimulator.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReaderService {


    public static final String XML_FILE_TYPE = "xml";
    public static final String JSON_FILE_TYPE = "json";
    private final GateProperties gateProperties;
    private final ResourceLoader resourceLoader;
    private final XmlMapper mapper;

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

    public SupplyChainConsignment readFromFile(final String file) throws IOException {
        Resource resource = tryOpenFile(file, XML_FILE_TYPE);
        if (!resource.exists()) {
            resource = tryOpenFile(file, JSON_FILE_TYPE);
        }
        if (resource.exists()) {
            final String result = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
            return mapper.readValue(result, SupplyChainConsignment.class);
        }

        return null;
    }

    private Resource tryOpenFile(final String path, final String ext) {
        final String filePath = String.join(".", path, ext);
        log.info("try to open file : {}", filePath);
        return resourceLoader.getResource(filePath);
    }
}
