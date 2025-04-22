package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class ReaderServiceTest {

    AutoCloseable openMocks;

    private ReaderService readerService;

    private final String pathTu = "/tmp/cda";

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .minSleep(1000)
                .maxSleep(2000)
                .cdaPath(pathTu)
                .ap(GateProperties.ApConfig.builder()
                        .url("url")
                        .password("password")
                        .username("username").build()).build();
        readerService = new ReaderService(gateProperties);
        File dossier = new File(pathTu);
        dossier.mkdir();
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    @Disabled("disabled temporarily")
    void uploadFileTest() throws UploadException {
        final MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "teest.xml",
                "teest.xml",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8));

        readerService.uploadFile(mockMultipartFile);
    }

    @Test
    void readFromFileXmlTest() throws IOException {
        final String data = """
                <consignment xmlns="http://efti.eu/v1/consignment/common"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://efti.eu/v1/consignment/common ../consignment-common.xsd">
                </consignment>
                """;
        final Resource resource = mock(Resource.class);
        Mockito.when(resource.exists()).thenReturn(false);
        Mockito.when(resource.exists()).thenReturn(true);
        Mockito.when(resource.getContentAsString(any())).thenReturn(data);
        final SupplyChainConsignment result = readerService.readFromFile("src/test/resources/teest", List.of("full"));

        Assertions.assertNotNull(result);
    }

    @Test
    void readFromFileXmlNullTest() throws IOException {
        final Resource resource = mock(Resource.class);
        Mockito.when(resource.exists()).thenReturn(false);
        Mockito.when(resource.exists()).thenReturn(false);
        final SupplyChainConsignment result = readerService.readFromFile("classpath:cda/bouuuuuuuuuuuuh", List.of("full"));

        Assertions.assertNull(result);
    }

    @Test
    void deleteAllFileTest() throws IOException {
        File file = new File(pathTu + "/test.xml");
        File file2 = new File(pathTu + "/test2.xml");
        File file3 = new File(pathTu + "/test3.xml");
        file.createNewFile();
        file2.createNewFile();
        file3.createNewFile();

        boolean result = readerService.deleteAllFile();

        Assertions.assertTrue(result);

        Assertions.assertEquals(file.getParentFile().list().length, 0);
    }

    @Test
    void deleteFileFalseTest() {
        final String file = "test";

        boolean result = readerService.deleteFile(file);

        Assertions.assertFalse(result);
    }

    @Test
    void deleteFileTest() throws IOException {
        File file = new File(pathTu + "/test.xml");
        file.createNewFile();
        final String fileString = "test";

        boolean result = readerService.deleteFile(fileString);

        Assertions.assertTrue(result);
    }
}
