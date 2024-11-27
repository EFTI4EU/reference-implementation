package eu.efti.eftigate.controller.api;

import eu.efti.commons.dto.UilDto;
import eu.efti.eftigate.config.security.Roles;
import eu.efti.eftigate.dto.RequestIdDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Request controller", description = "Interface to manage dataset request")
@RequestMapping("/v1")
public interface ControlControllerApi {

    @PostMapping("/control/uil")
    @Secured(Roles.ROLE_ROAD_CONTROLER)
    ResponseEntity<RequestIdDto> requestUil(@RequestBody UilDto uilDto);

    @Operation(summary = "Get an UIL request", description = "Get an UIL request for a given request uuid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema()))
    })
    @GetMapping("/control/uil")
    @Secured(Roles.ROLE_ROAD_CONTROLER)
    ResponseEntity<RequestIdDto> getRequestUil(@Parameter String requestId);
}
