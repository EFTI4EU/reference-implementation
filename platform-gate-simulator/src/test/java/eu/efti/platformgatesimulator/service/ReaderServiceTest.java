package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class ReaderServiceTest {

    AutoCloseable openMocks;

    private ReaderService readerService;

    private static final String PATH_TU = "/tmp/cda";

    @BeforeEach
    void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .minSleep(1000)
                .maxSleep(2000)
                .cdaPath(PATH_TU)
                .ap(GateProperties.ApConfig.builder()
                        .url("url")
                        .password("password")
                        .username("username").build()).build();
        readerService = new ReaderService(gateProperties);
        File dossier = new File(PATH_TU);
        dossier.mkdir();
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
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
        File file = new File(PATH_TU + "/test.xml");
        File file2 = new File(PATH_TU + "/test2.xml");
        File file3 = new File(PATH_TU + "/test3.xml");
        file.createNewFile();
        file2.createNewFile();
        file3.createNewFile();

        boolean result = readerService.deleteAllFile();

        Assertions.assertTrue(result);

        Assertions.assertEquals(0, file.getParentFile().list().length);
    }

    @Test
    void deleteFileFalseTest() {
        final String file = "test";

        boolean result = readerService.deleteFile(file);

        Assertions.assertFalse(result);
    }

    @Test
    void deleteFileTest() throws IOException {
        File file = new File(PATH_TU + "/test.xml");
        file.createNewFile();
        final String fileString = "test";

        boolean result = readerService.deleteFile(fileString);

        Assertions.assertTrue(result);
    }
}
