package eu.efti.eftigate.controller.aap;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.efti.commons.dto.AuthorityDto;
import eu.efti.commons.dto.ContactInformationDto;
import eu.efti.commons.dto.aap.AapUilDto;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AapControlController.class)
@ContextConfiguration(classes = {AapControlController.class})
@ExtendWith(SpringExtension.class)
class AapControlControllerTest {

    public static final String REQUEST_ID = "requestId";
    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    ControlService controlService;

    private final RequestIdDto requestIdDto = new RequestIdDto();

    @BeforeEach
    void before() {
        requestIdDto.setStatus(StatusEnum.PENDING);
        requestIdDto.setRequestId(REQUEST_ID);
    }


    @Test
    @WithMockUser
    void requestUilTestInvalidArgument() throws Exception {
        final AapUilDto uilDto = new AapUilDto();
        uilDto.setPlatformId("platform");
        uilDto.setDatasetId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        uilDto.setGateId("gate");

        Mockito.when(controlService.createUilControl(uilDto)).thenReturn(requestIdDto);

        mockMvc.perform(post("/v1/aap/control/uil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(uilDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @WithMockUser
    void requestUilTest() throws Exception {
        final AapUilDto uilDto = AapUilDto.builder().platformId("platform")
                .datasetId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .gateId("gate")
                .subsetIds(Arrays.asList("full"))
                .authority(AuthorityDto.builder().name("name").country("Country").isEmergencyService(false).nationalUniqueIdentifier("ID")
                .legalContact(ContactInformationDto.builder().email("mail").city("city").postalCode("pc").streetName("sn").additionalLine("al").build())
                .workingContact(ContactInformationDto.builder().email("mail").city("city").postalCode("pc").streetName("sn").additionalLine("al").build())
                .build()).build();

        Mockito.when(controlService.createUilControl(uilDto)).thenReturn(requestIdDto);

        String result=mockMvc.perform(post("/v1/aap/control/uil")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(uilDto)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        with(result)
                .assertThat("$.requestId", is("requestId"))
                .assertThat("$.status", is("PENDING"));
    }

    @Test
    @WithMockUser
    void getRequestUilTest() throws Exception {
        Mockito.when(controlService.getControlEntity(REQUEST_ID)).thenReturn(requestIdDto);

        final MvcResult result = mockMvc.perform(get("/v1/aap/control/uil").param("requestId", REQUEST_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        final String contentAsString = result.getResponse().getContentAsString();

        final RequestIdDto response = new ObjectMapper().readValue(contentAsString, RequestIdDto.class);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(REQUEST_ID, response.getRequestId());
    }
}
