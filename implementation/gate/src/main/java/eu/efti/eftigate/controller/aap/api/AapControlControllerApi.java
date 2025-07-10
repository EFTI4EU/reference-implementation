package eu.efti.eftigate.controller.aap.api;

import eu.efti.commons.dto.aap.AapUilDto;
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

@Tag(name = "AAP UIL query", description = "AAP Interface to manage dataset request")
@RequestMapping("/v1")
public interface AapControlControllerApi {

    @Operation(summary = "AAP send an UIL query", description = "AAP send a query for given UIL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema()))
    })
    @PostMapping("/aap/control/uil")
    @Secured(Roles.ROLE_EXT_AAP)
    ResponseEntity<RequestIdDto> requestUil(@RequestBody AapUilDto uilDto);

    @Operation(summary = "AAP get a response to an UIL query", description = "AAP get a dataset for a given request id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema()))
    })
    @GetMapping("/aap/control/uil")
    @Secured(Roles.ROLE_EXT_AAP)
    ResponseEntity<RequestIdDto> getRequestUil(@Parameter String requestId);
}
