package eu.efti.platformgatesimulator.controller;

import eu.efti.commons.dto.UilDto;
import eu.efti.platformgatesimulator.service.ControlService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@AllArgsConstructor
@Slf4j
public class ControlController {

    private final ControlService controlService;

        @PostMapping("/control/queryUIL")
    public ResponseEntity<UilDto> requestUil(@RequestBody final UilDto uilDto) {
        log.info("POST on /control/queryUIL with params gateId: {}, datasetId: {}, platformId: {}", uilDto.getGateId(), uilDto.getDatasetId(), uilDto.getPlatformId());
        controlService.sendRequestUil(uilDto);
        return new ResponseEntity<>(uilDto, HttpStatus.ACCEPTED);
    }
}
