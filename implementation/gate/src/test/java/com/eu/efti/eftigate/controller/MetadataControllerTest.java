package eu.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.dto.MetadataRequestDto;
import eu.efti.commons.dto.MetadataResponseDto;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.dto.RequestUuidDto;
import eu.efti.eftigate.service.ControlService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetadataController.class)
@ContextConfiguration(classes= {MetadataController.class})
@ExtendWith(SpringExtension.class)
class MetadataControllerTest {

    public static final String REQUEST_UUID = "requestUuid";

    private final MetadataResponseDto metadataResponseDto = new MetadataResponseDto();

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    ControlService controlService;
    @BeforeEach
    void before() {
        metadataResponseDto.setStatus(StatusEnum.COMPLETE);
        metadataResponseDto.setRequestUuid(REQUEST_UUID);
    }

    @Test
    @WithMockUser
    void requestUilTest() throws Exception {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder().vehicleID("abc123").build();

        Mockito.when(controlService.createMetadataControl(metadataRequestDto)).thenReturn(
                RequestUuidDto.builder()
                .status(StatusEnum.PENDING)
                .requestUuid(REQUEST_UUID)
                .build());

        mockMvc.perform(post("/v1/getMetadata")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(metadataRequestDto)))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    @Test
    @WithMockUser
    void requestUilGetTest() throws Exception {
        Mockito.when(controlService.getMetadataResponse(REQUEST_UUID)).thenReturn(metadataResponseDto);

        final MvcResult result = mockMvc.perform(get("/v1/getMetadata").param("requestUuid", REQUEST_UUID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        final String contentAsString = result.getResponse().getContentAsString();

        final MetadataResponseDto response = new ObjectMapper().readValue(contentAsString, MetadataResponseDto.class);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(REQUEST_UUID, response.getRequestUuid());
    }
}
