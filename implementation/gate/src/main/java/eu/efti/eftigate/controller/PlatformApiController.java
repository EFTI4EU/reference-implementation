package eu.efti.eftigate.controller;

import eu.efti.commons.dto.SaveIdentifiersRequestWrapper;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.controller.api.platform.V0Api;
import eu.efti.eftigate.dto.GetWhoami200Response;
import eu.efti.eftigate.service.request.ValidationService;
import eu.efti.identifiersregistry.service.IdentifiersService;
import eu.efti.v1.consignment.identifier.SupplyChainConsignment;
import eu.efti.v1.edelivery.SaveIdentifiersRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@Tag(name = "Platform API", description = "REST API for the platforms")
@AllArgsConstructor
public class PlatformApiController implements V0Api {
    private IdentifiersService identifiersService;

    private SerializeUtils serializeUtils;

    private ValidationService validationService;

    @Override
    public ResponseEntity<GetWhoami200Response> getWhoami() {
        var ctx = PlatformApiContextResolver.getPlatformContextOrFail();
        return ResponseEntity.ok(new GetWhoami200Response(ctx.platformId(), ctx.role()));
    }

    @Override
    public ResponseEntity<Void> putConsignmentIdentifiers(String datasetId, Object body) {
        var ctx = PlatformApiContextResolver.getPlatformContextOrFail();

        var xml = (String) body;
        var validationError = validationService.isXmlValid(xml);
        if (validationError.isPresent()) {
            var problemDetail = org.springframework.http.ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setDetail(validationError.get());
            return ResponseEntity.of(problemDetail).headers(h -> h.setContentType(MediaType.APPLICATION_PROBLEM_XML)).build();
        } else {
            SupplyChainConsignment consignment = serializeUtils.mapXmlStringToJaxbObject(xml, SupplyChainConsignment.class);

            SaveIdentifiersRequest saveIdentifiersRequest = new SaveIdentifiersRequest();
            saveIdentifiersRequest.setDatasetId(datasetId);
            saveIdentifiersRequest.setConsignment(consignment);
            identifiersService.createOrUpdate(new SaveIdentifiersRequestWrapper(ctx.platformId(), saveIdentifiersRequest));

            return ResponseEntity.ok().build();
        }
    }
}
