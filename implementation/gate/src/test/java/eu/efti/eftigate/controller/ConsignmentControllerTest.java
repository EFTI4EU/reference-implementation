package eu.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.enums.StatusEnum;
import eu.efti.eftigate.dto.RequestUuidDto;
import eu.efti.eftigate.service.ControlService;
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

@WebMvcTest(IdentifiersController.class)
@ContextConfiguration(classes= {IdentifiersController.class})
@ExtendWith(SpringExtension.class)
class ConsignmentControllerTest {

    public static final String REQUEST_UUID = "requestUuid";

    private final IdentifiersResponseDto identifiersResponseDto = new IdentifiersResponseDto();

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    ControlService controlService;
    @BeforeEach
    void before() {
        identifiersResponseDto.setStatus(StatusEnum.COMPLETE);
        identifiersResponseDto.setRequestUuid(REQUEST_UUID);
    }

    @Test
    @WithMockUser
    void requestIdentifiersTest() throws Exception {
        final SearchWithIdentifiersRequestDto identifiersRequestDto = SearchWithIdentifiersRequestDto.builder().vehicleID("abc123").build();

        Mockito.when(controlService.createIdentifiersControl(identifiersRequestDto)).thenReturn(
                RequestUuidDto.builder()
                .status(StatusEnum.PENDING)
                .requestUuid(REQUEST_UUID)
                .build());

        String result = mockMvc.perform(post("/v1/getIdentifiers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(identifiersRequestDto)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        with(result)
                .assertThat("$.requestUuid", is("requestUuid"))
                .assertThat("$.status", is("PENDING"));
    }

    @Test
    @WithMockUser
    void requestIdentifiersGetTest() throws Exception {
        Mockito.when(controlService.getIdentifiersResponse(REQUEST_UUID)).thenReturn(identifiersResponseDto);

        final String result = mockMvc.perform(get("/v1/getIdentifiers").param("requestUuid", REQUEST_UUID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        with(result)
                .assertThat("$.requestUuid", is("requestUuid"))
                .assertThat("$.status", is("COMPLETE"));
    }

    @Test
    @WithMockUser
    void requestIdentifiersNotFoundGetTest() throws Exception {
        identifiersResponseDto.setRequestUuid(null);
        identifiersResponseDto.setErrorCode("Uuid not found.");
        identifiersResponseDto.setErrorDescription("Error requestUuid not found.");
        Mockito.when(controlService.getIdentifiersResponse(REQUEST_UUID)).thenReturn(identifiersResponseDto);

        final String result = mockMvc.perform(get("/v1/getIdentifiers").param("requestUuid", REQUEST_UUID))
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
