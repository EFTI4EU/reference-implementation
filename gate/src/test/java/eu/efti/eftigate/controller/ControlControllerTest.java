package eu.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.dto.UilDto;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.entity.ControlEntity;
import eu.efti.eftigate.service.ControlService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControlController.class)
@ContextConfiguration(classes = {ControlController.class})
@ExtendWith(SpringExtension.class)
class ControlControllerTest {

    public static final String REQUEST_ID = "requestId";
    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    ControlService controlService;

    private final RequestIdDto requestIdDto = new RequestIdDto();

    @BeforeEach
    void before() {
        requestIdDto.setStatus(StatusEnum.PENDING);
        requestIdDto.setRequestId(REQUEST_ID);
    }

    @Test
    @WithAnonymousUser
    void getByIdshouldGetAuthent() throws Exception {
        Mockito.when(controlService.getById(1L)).thenReturn(new ControlEntity());

        mockMvc.perform(get("/v1/control/uil"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @WithMockUser
    void requestUilTest() throws Exception {
        final UilDto uilDto = new UilDto();
        uilDto.setPlatformId("platform");
        uilDto.setDatasetId("uuid");
        uilDto.setGateId("gate");

        Mockito.when(controlService.createUilControl(uilDto)).thenReturn(requestIdDto);

        mockMvc.perform(post("/v1/control/uil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(uilDto)))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    @Test
    @WithMockUser
    void getRequestUilTest() throws Exception {
        Mockito.when(controlService.getControlEntity(REQUEST_ID)).thenReturn(requestIdDto);

        final MvcResult result = mockMvc.perform(get("/v1/control/uil").param("requestId", REQUEST_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        final String contentAsString = result.getResponse().getContentAsString();

        final RequestIdDto response = new ObjectMapper().readValue(contentAsString, RequestIdDto.class);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(REQUEST_ID, response.getRequestId());
    }
}
