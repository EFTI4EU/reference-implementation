package eu.efti.platformgatesimulator.controller;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.platformgatesimulator.service.IdentifierService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@WebMvcTest(ControlController.class)
@ContextConfiguration(classes = {ControlController.class})
@ExtendWith(SpringExtension.class)
class ControlControllerTest {

    @MockitoBean
    private IdentifierService identifierService;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ControlController controlController;

    @Test
    void requestUilTest() {
        UilDto uilDto = UilDto.builder().platformId("platformId")
                .gateId("gateId")
                .datasetId("datasetId")
                .subsetIds(List.of("subsetId")).build();

        ResponseEntity<UilDto> response = controlController.requestUil(uilDto);

        verify(identifierService, times(1)).sendRequestUil(any());
        Assertions.assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    void getIdentifiersTest() {
        final SearchWithIdentifiersRequestDto searchWithIdentifiersRequestDto = new SearchWithIdentifiersRequestDto();

        ResponseEntity<SearchWithIdentifiersRequestDto> response = controlController.getIdentifiers(searchWithIdentifiersRequestDto);

        verify(identifierService, times(1)).sendIdentifierRequest(any());
        Assertions.assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

}
