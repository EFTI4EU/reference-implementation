package com.ingroupe.efti.eftigate.controller;

import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.service.ControlService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControlController.class)
@ContextConfiguration(classes= {ControlController.class})
@ExtendWith(SpringExtension.class)
class ControlControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    ControlService controlService;

    @Test
    @WithMockUser
    void getByIdTestWithData() throws Exception {
        Mockito.when(controlService.getById(1L)).thenReturn(new ControlEntity());

        mockMvc.perform(get("/control/1"))
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
}
