package eu.efti.platformgatesimulator.controller;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlatformApiForGateController.class)
@ContextConfiguration(classes = {PlatformApiForGateController.class})
@ExtendWith(SpringExtension.class)
class PlatformApiForGateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReaderService readerService;

    @MockitoBean
    private GateProperties gateProperties;

    @MockitoBean
    private SerializeUtils serializeUtils;

    private final String datasetId = "test-dataset";

    private final eu.efti.v1.consignment.common.SupplyChainConsignment dummyConsignment = new SupplyChainConsignment();

    @BeforeEach
    void setup() {
        when(gateProperties.getCdaPath()).thenReturn("/fake/path");
    }

    @Test
    @WithMockUser
    void getConsignmentsSubsets_shouldReturnXml_whenDataIsFound() throws Exception {
        when(readerService.readFromFile(anyString(), anyList())).thenReturn(dummyConsignment);

        String xmlString = "<xml>test</xml>";
        when(serializeUtils.mapJaxbObjectToXmlString(any(), any()))
                .thenReturn(xmlString);

        mockMvc.perform(get("/v0/consignments/{datasetId}", datasetId)
                        .param("subsetId", "one", "two"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().string(xmlString));
    }

    @Test
    @WithMockUser
    void getConsignmentsSubsets_shouldReturn404_whenDataNotFound() throws Exception {
        when(readerService.readFromFile(anyString(), anyList()))
                .thenReturn(null);

        mockMvc.perform(get("/v0/consignments/{datasetId}", datasetId)
                        .param("subsetId", "one"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getConsignmentsSubsets_shouldReturn500_onTechnicalException() throws Exception {
        when(readerService.readFromFile(anyString(), anyList()))
                .thenThrow(new TechnicalException("technical error"));

        mockMvc.perform(get("/v0/consignments/{datasetId}", datasetId)
                        .param("subsetId", "one"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("technical error"));
    }

    @Test
    @WithMockUser
    void getConsignmentsSubsets_shouldReturn400_onIllegalArgument() throws Exception {
        when(readerService.readFromFile(anyString(), anyList()))
                .thenThrow(new IllegalArgumentException("bad argument"));

        mockMvc.perform(get("/v0/consignments/{datasetId}", datasetId)
                        .param("subsetId", "one"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("bad argument"));
    }

    @Test
    @WithMockUser
    void postConsignmentFollowup_shouldReturn204() throws Exception {
        String body = "some follow-up note";

        mockMvc.perform(post("/v0/consignments/{datasetId}/follow-up", datasetId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .with(csrf())
                        .content(body))
                .andExpect(status().isNoContent());
    }
}
