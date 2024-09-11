package com.ingroupe.efti.eftigate.controller;

import com.ingroupe.efti.commons.dto.MetadataRequestDto;
import com.ingroupe.efti.commons.dto.MetadataResponseDto;
import com.ingroupe.efti.eftigate.controller.api.MetadataControllerApi;
import com.ingroupe.efti.eftigate.dto.RequestUuidDto;
import com.ingroupe.efti.eftigate.service.ControlService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@AllArgsConstructor
@Slf4j
public class MetadataController implements MetadataControllerApi {

    private final ControlService controlService;

    @Override
    public ResponseEntity<RequestUuidDto> getMetadata(final @RequestBody MetadataRequestDto metadataRequestDto) {
        log.info("POST on /getMetadata with param vehicleID {}", metadataRequestDto.getVehicleID());
        return new ResponseEntity<>(controlService.createMetadataControl(metadataRequestDto), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<MetadataResponseDto> getMetadataResult(final @Parameter String requestUuid) {
        log.info("GET on /getMetadata with param requestUuid {}", requestUuid);
        return new ResponseEntity<>(controlService.getMetadataResponse(requestUuid), HttpStatus.OK);
    }
}
