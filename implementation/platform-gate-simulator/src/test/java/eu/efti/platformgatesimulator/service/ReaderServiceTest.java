package eu.efti.platformgatesimulator.service;

import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.exception.UploadException;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class ReaderServiceTest {

    AutoCloseable openMocks;

    private ReaderService readerService;

    private ResourceLoader resourceLoader;

    @BeforeEach
    public void before() {
        resourceLoader = Mockito.mock(ResourceLoader.class);
        openMocks = MockitoAnnotations.openMocks(this);
        final GateProperties gateProperties = GateProperties.builder()
                .owner("france")
                .minSleep(1000)
                .maxSleep(2000)
                .cdaPath("classpath:cda/")
                .ap(GateProperties.ApConfig.builder()
                        .url("url")
                        .password("password")
                        .username("username").build()).build();
        readerService = new ReaderService(gateProperties, resourceLoader);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void uploadFileNullTest() {
        assertThrows(NullPointerException.class, () -> readerService.uploadFile(null));
    }

    @Test
    void uploadFileTest() throws IOException {
        final Resource resource = Mockito.mock(Resource.class);
        final URI uri = Mockito.mock(URI.class);
        Mockito.when(resourceLoader.getResource(any())).thenReturn(resource);
        Mockito.when(resource.getURI()).thenReturn(uri);
        Mockito.when(uri.getPath()).thenReturn("./cda/");
        final MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "teest.xml",
                "teest.xml",
                "text/plain",
                "content".getBytes());

        assertThrows(UploadException.class, () -> readerService.uploadFile(mockMultipartFile));
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
        Mockito.when(resourceLoader.getResource(any())).thenReturn(resource);
        Mockito.when(resource.exists()).thenReturn(false);
        Mockito.when(resource.exists()).thenReturn(true);
        Mockito.when(resource.getInputStream()).thenReturn(IOUtils.toInputStream(data, "UTF-8"));
        final SupplyChainConsignment result = readerService.readFromFile("classpath:cda/teest");

        Assertions.assertNotNull(result);
    }

    @Test
    void readFromFileXmlNullTest() throws IOException {
        final Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resourceLoader.getResource(any())).thenReturn(resource);
        Mockito.when(resource.exists()).thenReturn(false);
        Mockito.when(resource.exists()).thenReturn(false);
        final SupplyChainConsignment result = readerService.readFromFile("classpath:cda/bouuuuuuuuuuuuh");

        Assertions.assertNull(result);
    }
}
