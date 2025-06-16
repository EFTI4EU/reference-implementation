package eu.efti.platformgatesimulator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import eu.efti.platformgatesimulator.service.IdentifierService;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.v1.json.Consignment;
import eu.efti.v1.json.SaveIdentifiersRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@WebMvcTest(IdentifiersController.class)
@ContextConfiguration(classes = {IdentifiersController.class})
@ExtendWith(SpringExtension.class)
class IdentifiersControllerTest {

    @MockitoBean
    private IdentifiersController identifiersController;

    @Mock
    private ApIncomingService apIncomingService;

    @Mock
    private ReaderService readerService;

    @Mock
    private IdentifierService identifierService;

    private final SaveIdentifiersRequest saveIdentifiersRequest = new SaveIdentifiersRequest();

    @BeforeEach
    void before() {
        identifiersController = new IdentifiersController(apIncomingService, readerService, new SerializeUtils(new ObjectMapper()), identifierService);
        saveIdentifiersRequest.setRequestId("requestId");
        saveIdentifiersRequest.setConsignment(new Consignment());
        saveIdentifiersRequest.setDatasetId("datasetId");
    }

    @Test
    void uploadConsignmentTest() {
        byte[] inputArray = "Test String".getBytes();
        MockMultipartFile mockMultipartFile = new MockMultipartFile("tempFileName", inputArray);

        when(identifierService.uploadIdentifier(anyString(), any())).thenReturn(new ResponseEntity<>("oki", HttpStatus.OK));

        ResponseEntity<String> response = identifiersController.uploadConsignment("datasetId", mockMultipartFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void uploadFileTest() {
        MockMultipartFile file = new MockMultipartFile("data", "other-file-name.data", "text/plain", "some other type".getBytes(StandardCharsets.UTF_8));

        ResponseEntity<String> result = identifiersController.uploadFile(file);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("File saved", result.getBody());
    }

    @Test
    void uploadFileNullTest() {
        ResponseEntity<String> result = identifiersController.uploadFile(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Error, no file sent", result.getBody());
    }

    @Test
    void uploadFileThrowTest() throws UploadException {
        MockMultipartFile file = new MockMultipartFile("data", "other-file-name.data", "text/plain", "some other type".getBytes(StandardCharsets.UTF_8));
        doThrow(UploadException.class).when(readerService).uploadFile(file);

        ResponseEntity<String> result = identifiersController.uploadFile(file);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Error while uploading file null", result.getBody());
    }

    @Test
    void uploadIdentifiersTest() {
        final ResponseEntity<String> result = identifiersController.uploadIdentifiers(saveIdentifiersRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Identifiers uploaded", result.getBody());
    }

    @Test
    void uploadIdentifiersNullTest() {
        final ResponseEntity<String> result = identifiersController.uploadIdentifiers(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("No identifiers sent", result.getBody());
    }

    @Test
    void uploadIdentifiersThrowTest() throws JsonProcessingException {
        doThrow(JsonProcessingException.class).when(apIncomingService).uploadIdentifiers(any());
        final ResponseEntity<String> result = identifiersController.uploadIdentifiers(saveIdentifiersRequest);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("No identifiers sent, error in JSON process", result.getBody());
    }

    @Test
    void deleteAllTest() {
        when(readerService.deleteAllFile()).thenReturn(true);

        final ResponseEntity<String> result = identifiersController.deleteAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("All file deleted", result.getBody());
    }

    @Test
    void deleteAllFalseTest() {
        when(readerService.deleteAllFile()).thenReturn(false);

        final ResponseEntity<String> result = identifiersController.deleteAll();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Error when try to delete all files", result.getBody());
    }

    @Test
    void deleteFileTest() {
        when(readerService.deleteFile(anyString())).thenReturn(true);

        final ResponseEntity<String> result = identifiersController.deleteFile("deleteFile");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("File with uuid deleteFile deleted", result.getBody());
    }

    @Test
    void deleteFileFalseTest() {
        when(readerService.deleteFile(anyString())).thenReturn(false);

        final ResponseEntity<String> result = identifiersController.deleteFile("deleteFile");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Error file deleteFile does not exist", result.getBody());
    }

}
