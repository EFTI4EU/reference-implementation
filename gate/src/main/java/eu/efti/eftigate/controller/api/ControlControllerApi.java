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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "UIL query", description = "Interface to manage dataset request")
@RequestMapping("/v1")
public interface ControlControllerApi {

    @Operation(summary = "Send an UIL query", description = "Send a query for given UIL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema()))
    })
    @PostMapping("/control/uil")
    @Secured(Roles.ROLE_ROAD_CONTROLER)
    ResponseEntity<RequestIdDto> requestUil(@RequestBody UilDto uilDto, @AuthenticationPrincipal Jwt principal);

    @Operation(summary = "Get a response to an UIL query", description = "Get a dataset for a given request id")
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
