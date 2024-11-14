package eu.efti.platformgatesimulator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.v1.json.Consignment;
import eu.efti.v1.json.SaveIdentifiersRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(IdentifiersController.class)
@ContextConfiguration(classes= {IdentifiersController.class})
@ExtendWith(SpringExtension.class)
class IdentifiersControllerTest {

    @MockBean
    private IdentifiersController identifiersController;

    @Autowired
    protected MockMvc mockMvc;

    @Mock
    private ApIncomingService apIncomingService;

    @Mock
    private ReaderService readerService;

    private final SaveIdentifiersRequest saveIdentifiersRequest = new SaveIdentifiersRequest();

    @BeforeEach
    void before() {
        identifiersController = new IdentifiersController(apIncomingService, readerService);
        saveIdentifiersRequest.setRequestId("requestId");
        saveIdentifiersRequest.setConsignment(new Consignment());
        saveIdentifiersRequest.setDatasetId("datasetId");
    }

    @Test
    void uploadFileTest() {
        MockMultipartFile file = new MockMultipartFile("data", "other-file-name.data", "text/plain", "some other type".getBytes(StandardCharsets.UTF_8));

        ResponseEntity<String> result = identifiersController.uploadFile(file);

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals("File saved", result.getBody());
    }

    @Test
    void uploadFileNullTest() {
        ResponseEntity<String> result = identifiersController.uploadFile(null);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Assertions.assertEquals("Error, no file sent", result.getBody());
    }

    @Test
    void uploadFileThrowTest() throws UploadException {
        MockMultipartFile file = new MockMultipartFile("data", "other-file-name.data", "text/plain", "some other type".getBytes(StandardCharsets.UTF_8));
        Mockito.doThrow(UploadException.class).when(readerService).uploadFile(file);

        ResponseEntity<String> result = identifiersController.uploadFile(file);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        Assertions.assertEquals("Error while uploading file null", result.getBody());
    }

    @Test
    void uploadIdentifiersTest() {
        final ResponseEntity<String> result = identifiersController.uploadIdentifiers(saveIdentifiersRequest);

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals("Identifiers uploaded", result.getBody());
    }

    @Test
    void uploadIdentifiersNullTest() {
        final ResponseEntity<String> result = identifiersController.uploadIdentifiers(null);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Assertions.assertEquals("No identifiers sent", result.getBody());
    }

    @Test
    void uploadIdentifiersThrowTest() throws JsonProcessingException {
        Mockito.doThrow(JsonProcessingException.class).when(apIncomingService).uploadIdentifiers(any());
        final ResponseEntity<String> result = identifiersController.uploadIdentifiers(saveIdentifiersRequest);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Assertions.assertEquals("No identifiers sent, error in JSON process", result.getBody());
    }

}
