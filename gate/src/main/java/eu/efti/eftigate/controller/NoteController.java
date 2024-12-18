package eu.efti.eftigate.controller;

import eu.efti.commons.dto.PostFollowUpRequestDto;
import eu.efti.eftigate.controller.api.NoteControllerApi;
import eu.efti.eftigate.dto.NoteResponseDto;
import eu.efti.eftigate.service.ControlService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@AllArgsConstructor
@Slf4j
public class NoteController implements NoteControllerApi {

    private final ControlService controlService;

    @Override
    public ResponseEntity<NoteResponseDto> createNote(final @RequestBody PostFollowUpRequestDto notesDto) {
        log.info("POST on /control/uil/follow-up with param requestId {}", notesDto.getRequestId());
        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);
        return new ResponseEntity<>(noteResponseDto, StringUtils.isNotBlank(noteResponseDto.getErrorCode()) ? HttpStatus.BAD_REQUEST : HttpStatus.ACCEPTED);
    }
}
