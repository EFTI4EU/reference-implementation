package eu.efti.platformgatesimulator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import eu.efti.platformgatesimulator.service.IdentifierService;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.v1.json.SaveIdentifiersRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/identifiers")
@AllArgsConstructor
@Slf4j
public class IdentifiersController {

    private final ApIncomingService apIncomingService;

    private final ReaderService readerService;

    private final SerializeUtils serializeUtils;

    private static final Pattern datasetIdPattern = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    private final IdentifierService identifierService;


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

    @DeleteMapping("/file/all")
    public ResponseEntity<String> deleteAll() {
        boolean result = readerService.deleteAllFile();
        return new ResponseEntity<>(result ? "All file deleted" : "Error when try to delete all files", result ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/file/{uuid}")
    public ResponseEntity<String> deleteFile(@PathVariable("uuid") String uuid) {
        boolean result = readerService.deleteFile(uuid);
        return new ResponseEntity<>(result ? "File with uuid " + uuid + " deleted" : "Error file " + uuid + " does not exist", result ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @PutMapping("/upload/consignment/{datasetId}")
    public ResponseEntity<String> uploadConsignment(@PathVariable String datasetId, @RequestPart final MultipartFile consignmentFile) {
        return identifierService.uploadIdentifier(datasetId, consignmentFile);
    }
}
