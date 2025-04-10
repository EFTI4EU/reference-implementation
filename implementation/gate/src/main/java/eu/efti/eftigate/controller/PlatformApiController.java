package eu.efti.eftigate.controller;

import eu.efti.eftigate.controller.api.platform.V0Api;
import eu.efti.eftigate.dto.GetWhoami200Response;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@Tag(name = "Platform API", description = "REST API for the platforms")
public class PlatformApiController implements V0Api {
    @Override
    public ResponseEntity<GetWhoami200Response> getWhoami() {
        var ctx = PlatformApiContextResolver.getPlatformContextOrFail();
        return ResponseEntity.ok(new GetWhoami200Response(ctx.platformId(), ctx.role()));
    }

    @Override
    public ResponseEntity<Void> putConsignmentIdentifiers(String datasetId, Object body) {
        return null;
    }
}
