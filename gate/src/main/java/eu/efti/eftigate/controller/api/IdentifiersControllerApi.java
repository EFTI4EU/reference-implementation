package eu.efti.eftigate.controller.api;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.eftigate.config.security.Roles;
import eu.efti.eftigate.dto.RequestUuidDto;
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

@Tag(name = "Identifiers controller" , description = "Interface to search with Identifiers")
@RequestMapping("/v1")
public interface IdentifiersControllerApi {

    @Operation(summary = "Send Search Request", description = "Send a search request to retrieve an efti data with Identifiers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema()))
    })
    @PostMapping("/getIdentifiers")
    @Secured(Roles.ROLE_ROAD_CONTROLER)
    ResponseEntity<RequestUuidDto> getIdentifiers(final @RequestBody SearchWithIdentifiersRequestDto identifiersRequestDto);

    @Operation(summary = "Get an identifiers request", description = "Get a identifiers request for a given request uuid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema()))
    })
    @GetMapping("/getIdentifiers")
    @Secured(Roles.ROLE_ROAD_CONTROLER)
    ResponseEntity<IdentifiersResponseDto> getIdentifiersResult(final @Parameter String requestUuid);

}
