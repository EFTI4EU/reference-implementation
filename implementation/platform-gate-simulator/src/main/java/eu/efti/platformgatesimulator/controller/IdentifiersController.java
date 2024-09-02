package eu.efti.platformgatesimulator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.commons.dto.ConsignmentIdentifiersDTO;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import eu.efti.platformgatesimulator.service.ReaderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/identifiers")
@AllArgsConstructor
@Slf4j
public class IdentifiersController {

    private final ApIncomingService apIncomingService;

    private final ReaderService readerService;

    @PostMapping("/upload/file")
    public ResponseEntity<String> uploadFile(@RequestPart final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("No file send");
            return new ResponseEntity("Error, no file send", HttpStatus.BAD_REQUEST);
        }
        log.info("try to upload file");
        try {
            readerService.uploadFile(file);
        } catch (UploadException e) {
            return new ResponseEntity<>("Error while uploading file " + e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("File saved",HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadIdentifiers(@RequestBody final ConsignmentIdentifiersDTO consignmentIdentifiersDTO) {
        if (consignmentIdentifiersDTO == null) {
            log.error("Error no identifiers send");
            return new ResponseEntity<>("No identifiers send", HttpStatus.BAD_REQUEST);
        }
        log.info("send identifiers to gate");
        try {
            apIncomingService.uploadIdentifiers(consignmentIdentifiersDTO);
        } catch (final JsonProcessingException e) {
            log.error("Error when try to send to gate the Identifiers", e);
            return new ResponseEntity<>("No identifiers send, error in JSON process", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Identifiers uploaded", HttpStatus.OK);
    }
}
