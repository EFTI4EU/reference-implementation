package eu.efti.platformgatesimulator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.utils.EftiSchemaUtils;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.platformgatesimulator.service.ApIncomingService;
import eu.efti.platformgatesimulator.service.GateIntegrationService;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static eu.efti.testsupport.EntityFactory.newConsignmentCommon;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@WebMvcTest(IdentifiersController.class)
@ContextConfiguration(classes = {IdentifiersController.class})
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

    @Mock
    private SerializeUtils serializeUtils;

    @Mock
    private GateIntegrationService gateIntegrationService;

    private final SaveIdentifiersRequest saveIdentifiersRequest = new SaveIdentifiersRequest();

    private static MockMultipartFile randomXmlMultipartFile() {
        return xmlMultipartFile("<some-xml/>");
    }

    private static MockMultipartFile xmlMultipartFile(String content) {
        return new MockMultipartFile("some-file.xml", "some-original-filename.xml",
                "application/xml", content.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void before() {
        identifiersController = new IdentifiersController(apIncomingService, readerService, serializeUtils, gateIntegrationService);
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

    @Test
    void uploadConsignmentHappyPathTest() {
        var actualSerializeUtils = new SerializeUtils(new ObjectMapper());

        var common = newConsignmentCommon();
        var file = xmlMultipartFile(actualSerializeUtils.mapDocToXmlString(EftiSchemaUtils.mapCommonObjectToDoc(actualSerializeUtils, common), true));
        var datasetId = UUID.randomUUID().toString();

        // Testing happy path requires actual serialization implementation
        ResponseEntity<String> result = new IdentifiersController(apIncomingService, readerService, actualSerializeUtils, gateIntegrationService)
                .uploadConsignment(datasetId, file);

        Assertions.assertAll(
                () -> Assertions.assertEquals(HttpStatus.OK, result.getStatusCode()),
                () -> Assertions.assertEquals("Consignment saved and identifiers uploaded to gate", result.getBody())
        );

        try {
            verify(gateIntegrationService, times(1)).uploadIdentifiers(
                    eq(datasetId),
                    argThat(identifier -> {
                        // Make some minimal assertion that verifies the identifier object is created from common object.
                        return identifier.getCarrierAcceptanceDateTime().getValue().equals(common.getCarrierAcceptanceDateTime().getValue());
                    }));
            verify(readerService, times(1)).uploadFile(file, "%s.xml".formatted(datasetId));
        } catch (GateIntegrationService.GateIntegrationServiceException | UploadException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void uploadConsignmentNullFileTest() {
        final ResponseEntity<String> result = identifiersController.uploadConsignment(null, null);

        Assertions.assertAll(
                () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode()),
                () -> Assertions.assertEquals("File is missing", result.getBody())
        );
    }

    @Test
    void uploadConsignmentNullDatasetIdTest() {
        final ResponseEntity<String> result = identifiersController.uploadConsignment(null, randomXmlMultipartFile());

        Assertions.assertAll(
                () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode()),
                () -> Assertions.assertEquals("Dataset ID is not valid", result.getBody())
        );
    }

    @Test
    void uploadConsignmentInvalidDatasetIdTest() {
        final ResponseEntity<String> result = identifiersController.uploadConsignment("sone-invalid-dataset-id", randomXmlMultipartFile());

        Assertions.assertAll(
                () -> Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode()),
                () -> Assertions.assertEquals("Dataset ID is not valid", result.getBody())
        );
    }
}
