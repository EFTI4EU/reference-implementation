package com.ingroupe.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingroupe.efti.commons.dto.UilDto;
import com.ingroupe.efti.commons.enums.StatusEnum;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
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

@WebMvcTest(ControlController.class)
@ContextConfiguration(classes= {ControlController.class})
@ExtendWith(SpringExtension.class)
class ControlControllerTest {

    public static final String REQUEST_UUID = "requestUuid";
    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    ControlService controlService;

    private final RequestUuidDto requestUuidDto = new RequestUuidDto();

    @BeforeEach
    void before() {
        requestUuidDto.setStatus(StatusEnum.PENDING);
        requestUuidDto.setRequestUuid(REQUEST_UUID);
    }

    @Test
    @WithMockUser
    void getByIdTestWithData() throws Exception {
        Mockito.when(controlService.getById(1L)).thenReturn(new ControlEntity());

        mockMvc.perform(get("/v1/1"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @WithAnonymousUser
    void getByIdshouldGetAuthent() throws Exception {
        Mockito.when(controlService.getById(1L)).thenReturn(new ControlEntity());

        mockMvc.perform(get("/control/1"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @WithMockUser
    void requestUilTest() throws Exception {
        final UilDto uilDto = new UilDto();
        uilDto.setEFTIPlatformUrl("platform");
        uilDto.setEFTIDataUuid("uuid");
        uilDto.setEFTIGateUrl("gate");

        Mockito.when(controlService.createUilControl(uilDto)).thenReturn(requestUuidDto);

        mockMvc.perform(post("/v1/requestUil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(uilDto)))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    @Test
    @WithMockUser
    void getRequestUilTest() throws Exception {
        Mockito.when(controlService.getControlEntity(REQUEST_UUID)).thenReturn(requestUuidDto);

        final MvcResult result = mockMvc.perform(get("/v1/requestUil").param("requestUuid", REQUEST_UUID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        final String contentAsString = result.getResponse().getContentAsString();

        final RequestUuidDto response = new ObjectMapper().readValue(contentAsString, RequestUuidDto.class);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(REQUEST_UUID, response.getRequestUuid());
    }
}
