package eu.efti.eftigate.controller.aap.api;

import eu.efti.commons.dto.PostFollowUpRequestDto;
import eu.efti.eftigate.config.security.Roles;
import eu.efti.eftigate.dto.NoteResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "AAP Follow up communication" , description = "AAP Interface to send a follow up communication to a platform ")
@RequestMapping("/v1")
public interface AapNoteControllerApi {

    @Operation(summary = "AAP Send follow up communication", description = "AAP Send a follow up communication to a platform for a given control")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/aap/control/uil/follow-up")
    @Secured(Roles.ROLE_EXT_AAP)
    ResponseEntity<NoteResponseDto> createNote(final @RequestBody PostFollowUpRequestDto notesDto);
}
