package com.ingroupe.efti.eftigate.controller;

import com.ingroupe.efti.commons.dto.UilDto;
import com.ingroupe.efti.eftigate.controller.api.ControlControllerApi;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.entity.ControlEntity;
import com.ingroupe.efti.eftigate.service.ControlService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@AllArgsConstructor
@Slf4j
public class ControlController implements ControlControllerApi {

    private final ControlService controlService;

    @Override
    public ResponseEntity<ControlEntity> getById(@PathVariable final long id) {
        return new ResponseEntity<>(controlService.getById(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RequestUuidDto> requestUil(@RequestBody final UilDto uilDto) {
        log.info("POST on /requestUil with param uuid {}", uilDto.getEFTIDataUuid());
        return new ResponseEntity<>(controlService.createUilControl(uilDto), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<RequestUuidDto> getRequestUil(@Parameter final String requestUuid) {
        log.info("GET on /requestUil with param requestUuid {}", requestUuid);
        return new ResponseEntity<>(controlService.getControlEntity(requestUuid), HttpStatus.OK);
    }
}
