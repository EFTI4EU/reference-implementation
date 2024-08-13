package com.ingroupe.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.service.ControlService;
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

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.core.Is.is;
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
    void requestMetadataTest() throws Exception {
        final MetadataRequestDto metadataRequestDto = MetadataRequestDto.builder().vehicleID("abc123").build();

        Mockito.when(controlService.createMetadataControl(metadataRequestDto)).thenReturn(
                RequestUuidDto.builder()
                .status(StatusEnum.PENDING)
                .requestUuid(REQUEST_UUID)
                .build());

        final String result = mockMvc.perform(post("/v1/getMetadata")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(metadataRequestDto)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        with(result)
                .assertThat("$.requestUuid", is("requestUuid"))
                .assertThat("$.status", is("PENDING"));
    }

    @Test
    @WithMockUser
    void requestMetadataGetTest() throws Exception {
        Mockito.when(controlService.getMetadataResponse(REQUEST_UUID)).thenReturn(metadataResponseDto);

        final String result = mockMvc.perform(get("/v1/getMetadata").param("requestUuid", REQUEST_UUID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        with(result)
                .assertThat("$.requestUuid", is("requestUuid"))
                .assertThat("$.status", is("COMPLETE"));
    }

    @Test
    @WithMockUser
    void requestMetadataNotFoundGetTest() throws Exception {
        metadataResponseDto.setRequestUuid(null);
        metadataResponseDto.setErrorCode("Uuid not found.");
        metadataResponseDto.setErrorDescription("Error requestUuid not found.");
        Mockito.when(controlService.getMetadataResponse(REQUEST_UUID)).thenReturn(metadataResponseDto);

        final String result = mockMvc.perform(get("/v1/getMetadata").param("requestUuid", REQUEST_UUID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        with(result)
                .assertThat("$.errorCode", is("Uuid not found."))
                .assertThat("$.errorDescription", is("Error requestUuid not found."))
                .assertThat("$.status", is("COMPLETE"));
        ;
    }
}
