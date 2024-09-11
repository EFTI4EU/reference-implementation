package com.ingroupe.efti.eftigate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingroupe.efti.commons.dto.ControlDto;
import com.ingroupe.efti.commons.dto.NotesDto;
import com.ingroupe.efti.eftigate.dto.NoteResponseDto;
import com.ingroupe.efti.eftigate.service.ControlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
@ContextConfiguration(classes= {NoteController.class})
@ExtendWith(SpringExtension.class)
class NoteControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    ControlService controlService;

    @Test
    @WithMockUser
    void createNoteTestAccepted() throws Exception {
        final NotesDto notesDto = new NotesDto();
        notesDto.setEFTIPlatformUrl("platform");
        notesDto.setEFTIDataUuid("uuid");
        notesDto.setEFTIGateUrl("gate");
        notesDto.setRequestUuid("requestUuid");
        notesDto.setNote("Conducteur suspect");

        when(controlService.getControlByRequestUuid("requestUuid")).thenReturn(new ControlDto());
        when(controlService.createNoteRequestForControl(notesDto)).thenReturn(NoteResponseDto.builder().message("Note sent").build());

        final String response = mockMvc.perform(post("/v1/notes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(notesDto)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        Mockito.verify(controlService).createNoteRequestForControl(notesDto);
        with(response).assertThat("$.message", is("Note sent"));
    }

    @Test
    @WithMockUser
    void createNoteTestNotAccepted() throws Exception {
        final NotesDto notesDto = new NotesDto();
        notesDto.setEFTIPlatformUrl("platform");
        notesDto.setEFTIDataUuid("uuid");
        notesDto.setEFTIGateUrl("gate");
        notesDto.setRequestUuid("requestUuid");
        notesDto.setNote("Conducteur suspect");

        when(controlService.createNoteRequestForControl(notesDto)).thenReturn(new NoteResponseDto("Note was not sent", "UUID_NOT_FOUND", "Uuid not found"));

        final String response = mockMvc.perform(post("/v1/notes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsBytes(notesDto)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        Mockito.verify(controlService).createNoteRequestForControl(notesDto);
        with(response).assertThat("$.message", is("Note was not sent"));
        with(response).assertThat("$.errorCode", is("UUID_NOT_FOUND"));
        with(response).assertThat("$.errorDescription", is("Uuid not found"));
    }
}
