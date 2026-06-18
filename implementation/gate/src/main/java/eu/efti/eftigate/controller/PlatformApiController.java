package eu.efti.eftigate.controller;

import eu.efti.eftigate.controller.api.platform.V0Api;
import eu.efti.eftigate.dto.GetWhoami200Response;
import eu.efti.eftigate.service.request.IdentifiersRequestService;
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
    private IdentifiersRequestService identifiersRequestService;

    @Override
    public ResponseEntity<GetWhoami200Response> getWhoami() {
        var ctx = PlatformApiContextResolver.getPlatformContextOrFail();
        return ResponseEntity.ok(new GetWhoami200Response(ctx.platformId(), ctx.role()));
    }

    @Override
    public ResponseEntity<Void> putConsignmentIdentifiers(String datasetId, Object body) {
        var ctx = PlatformApiContextResolver.getPlatformContextOrFail();
        var result = identifiersRequestService.createOrUpdateFromRest((String) body, datasetId, ctx.platformId());
        if (result.isPresent()) {
            var problemDetail = org.springframework.http.ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setDetail(result.get());
            return ResponseEntity.of(problemDetail).headers(h -> h.setContentType(MediaType.APPLICATION_PROBLEM_XML)).build();
        }
        return ResponseEntity.ok().build();
    }
}
