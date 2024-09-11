package com.ingroupe.efti.eftigate.controller;

import com.ingroupe.efti.commons.dto.NotesDto;
import com.ingroupe.efti.eftigate.controller.api.NoteControllerApi;
import com.ingroupe.efti.eftigate.dto.NoteResponseDto;
import com.ingroupe.efti.eftigate.service.ControlService;
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
    public ResponseEntity<NoteResponseDto> createNote(final @RequestBody NotesDto notesDto) {
        log.info("POST on /notes with param requestUuid {}", notesDto.getRequestUuid());
        log.info("sending note to platform {}", notesDto.getEFTIPlatformUrl());
        final NoteResponseDto noteResponseDto = controlService.createNoteRequestForControl(notesDto);
        return new ResponseEntity<>(noteResponseDto, StringUtils.isNotBlank(noteResponseDto.getErrorCode())? HttpStatus.BAD_REQUEST : HttpStatus.ACCEPTED);
    }
}
