package eu.efti.eftigate.controller;

import eu.efti.eftigate.service.ValidationService;
import eu.efti.eftigate.service.gate.EftiPlatformIdResolver;
import eu.efti.identifiersregistry.service.IdentifiersService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.xml.sax.SAXException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(IdentifiersRestController.class)
@ContextConfiguration(classes = {IdentifiersRestController.class})
@ExtendWith(SpringExtension.class)
class IdentifiersRestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private IdentifiersService identifiersService;

    @MockitoBean
    private EftiPlatformIdResolver eftiPlatformIdResolver;

    @Test
    void putConsignmentIdentifiersTest() throws Exception {

        Jwt mockJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "username")
                .claim("azp", "clientId")
                .build();

        JwtRequestPostProcessor jwtRequest = jwt().jwt(mockJwt);

        String xmlBody = """
                <?xml version="1.0"?>
                """;

        when(eftiPlatformIdResolver.getPlatformIdOrFail(Mockito.any())).thenReturn("acme");
        doNothing().when(validationService).validateXml(Mockito.any());
        doNothing().when(identifiersService).createOrUpdateConsignment(xmlBody, "1234", "acme");

        mockMvc.perform(MockMvcRequestBuilders.put("/v0/consignments/{datasetId}", "1234")
                        .with(jwtRequest)
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xmlBody))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void putConsignmentIdentifiersTest_whenThrowException() throws Exception {

        Jwt mockJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "username")
                .claim("azp", "clientId")
                .build();

        JwtRequestPostProcessor jwtRequest = jwt().jwt(mockJwt);

        String xmlBody = """
                <?xml version="1.0"?>
                """;

        when(eftiPlatformIdResolver.getPlatformIdOrFail(Mockito.any())).thenReturn("acme");
        doThrow(new SAXException("Error occurred")).when(validationService).validateXml(anyString());
        doNothing().when(identifiersService).createOrUpdateConsignment(xmlBody, "1234", "acme");

        mockMvc.perform(MockMvcRequestBuilders.put("/v0/consignments/{datasetId}", "1234")
                        .with(jwtRequest)
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xmlBody))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

}
