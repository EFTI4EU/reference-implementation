package com.ingroupe.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.service.ControlService;
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
        metadataResponseDto.setStatus("COMPLETED");
        metadataResponseDto.setRequestUuid(REQUEST_UUID);
    }

    @Test
    @WithMockUser
    void requestUilTest() throws Exception {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder().vehicleID("abc123").build();

        Mockito.when(controlService.createMetadataControl(metadataRequestDto)).thenReturn(
                RequestUuidDto.builder()
                .status("PENDING")
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
        Mockito.when(controlService.getControlEntityForMetadata(REQUEST_UUID)).thenReturn(metadataResponseDto);

        MvcResult result = mockMvc.perform(get("/v1/getMetadata").param("requestUuid", REQUEST_UUID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        String contentAsString = result.getResponse().getContentAsString();

        MetadataResponseDto response = new ObjectMapper().readValue(contentAsString, MetadataResponseDto.class);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(REQUEST_UUID, response.getRequestUuid());
    }
}