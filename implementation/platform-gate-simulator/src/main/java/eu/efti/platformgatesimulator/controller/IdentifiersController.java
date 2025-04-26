package eu.efti.platformgatesimulator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.MappingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.datatools.schema.EftiSchemas;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import eu.efti.platformgatesimulator.service.GateIntegrationService;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.platformgatesimulator.utils.PlatformEftiSchemaUtils;
import eu.efti.v1.json.SaveIdentifiersRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/identifiers")
@AllArgsConstructor
@Slf4j
public class IdentifiersController {
    private static final Pattern datasetIdPattern = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

    private final ApIncomingService apIncomingService;

    private final ReaderService readerService;

    private final SerializeUtils serializeUtils;

    private final GateIntegrationService gateIntegrationService;

    @PostMapping("/upload/file")
    public ResponseEntity<String> uploadFile(@RequestPart final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("No file sent");
            return new ResponseEntity<>("Error, no file sent", HttpStatus.BAD_REQUEST);
        }
        log.info("try to upload file");
        try {
            readerService.uploadFile(file);
        } catch (UploadException e) {
            return new ResponseEntity<>("Error while uploading file " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("File saved", HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadIdentifiers(@RequestBody final SaveIdentifiersRequest identifiersDto) {
        if (identifiersDto == null) {
            log.error("Error no identifiers sent");
            return new ResponseEntity<>("No identifiers sent", HttpStatus.BAD_REQUEST);
        }
        log.info("send identifiers to gate");
        try {
            apIncomingService.uploadIdentifiers(identifiersDto);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to send to gate the Identifiers", e);
            return new ResponseEntity<>("No identifiers sent, error in JSON process", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Identifiers uploaded", HttpStatus.OK);
    }

    @PutMapping("/upload/consignment/{datasetId}")
    public ResponseEntity<String> uploadConsignment(
            @PathVariable String datasetId,
            @RequestPart final MultipartFile consignmentFile) {
        if (consignmentFile == null || consignmentFile.isEmpty()) {
            return new ResponseEntity<>("File is missing", HttpStatus.BAD_REQUEST);
        }
        if (datasetId == null || !datasetIdPattern.matcher(datasetId).matches()) {
            return new ResponseEntity<>("Dataset ID is not valid", HttpStatus.BAD_REQUEST);
        }

        // TODO: save common
        log.info("Upload consignment {}", datasetId);
        var commonXml = readFileAsString(consignmentFile);
        try {
            var common = serializeUtils.mapXmlStringToJaxbObject(commonXml, eu.efti.v1.consignment.common.SupplyChainConsignment.class, EftiSchemas.getJavaCommonSchema());
            var identifiers = PlatformEftiSchemaUtils.commonToIdentifiers(serializeUtils, common);
            gateIntegrationService.uploadIdentifiers(datasetId, identifiers);
        } catch (MappingException e) {
            log.error("Could not map xml object", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return new ResponseEntity<>("Consignment saved and identifiers uploaded to gate", HttpStatus.OK);
    }

    private static String readFileAsString(MultipartFile file) {
        try (var inputStream = file.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TechnicalException("Error while reading attachment", e);
        }
    }
}
