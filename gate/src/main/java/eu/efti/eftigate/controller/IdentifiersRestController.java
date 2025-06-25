package eu.efti.eftigate.controller;

import eu.efti.eftigate.service.ValidationService;
import eu.efti.eftigate.service.gate.EftiPlatformIdResolver;
import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import java.io.IOException;

@RestController
@AllArgsConstructor
@Slf4j
public class IdentifiersRestController {

    private final ValidationService validationService;

    private final IdentifiersService identifiersService;

    private final EftiPlatformIdResolver eftiPlatformIdResolver;

    @PutMapping(
            value = "/v0/consignments/{datasetId}",
            consumes = {"application/xml"}
    )
    public ResponseEntity<Void> putConsignmentIdentifiers(@PathVariable("datasetId") String datasetId, @RequestBody String body, final @AuthenticationPrincipal Jwt jwt) {
        String platformId = eftiPlatformIdResolver.getPlatformIdOrFail(jwt);
        try {
            validationService.validateXml(body);
            identifiersService.createOrUpdateConsignment(body, datasetId, platformId);
            return ResponseEntity.noContent().build();
        } catch (SAXException | IOException e) {
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setDetail(e.getMessage());
            return ResponseEntity.of(problemDetail).headers(h -> h.setContentType(MediaType.APPLICATION_PROBLEM_XML)).build();
        }
    }
}
