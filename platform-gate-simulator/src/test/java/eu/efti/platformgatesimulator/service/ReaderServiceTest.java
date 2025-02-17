package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

class ReaderServiceTest {

    AutoCloseable openMocks;

    private ReaderService readerService;

    @BeforeEach
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .minSleep(1000)
                .maxSleep(2000)
                .cdaPath("/opt/javapp/test")
                .ap(GateProperties.ApConfig.builder()
                        .url("url")
                        .password("password")
                        .username("username").build()).build();
        readerService = new ReaderService(gateProperties);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    @Disabled
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
        final Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.exists()).thenReturn(false);
        Mockito.when(resource.exists()).thenReturn(true);
        Mockito.when(resource.getContentAsString(any())).thenReturn(data);
        final SupplyChainConsignment result = readerService.readFromFile("src/test/resources/teest", List.of("full"));

        Assertions.assertNotNull(result);
    }

    @Test
    void readFromFileXmlNullTest() throws IOException {
        final Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.exists()).thenReturn(false);
        Mockito.when(resource.exists()).thenReturn(false);
        final SupplyChainConsignment result = readerService.readFromFile("classpath:cda/bouuuuuuuuuuuuh", List.of("full"));

        Assertions.assertNull(result);
    }
}
