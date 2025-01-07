package eu.efti.platformgatesimulator.controller;

import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.commons.dto.UilDto;
import eu.efti.platformgatesimulator.service.IdentifierService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private final IdentifierService identifierService;

    @PostMapping("/control/queryUIL")
    public ResponseEntity<UilDto> requestUil(@RequestBody final UilDto uilDto) {
        log.info("POST on /control/queryUIL with params gateId: {}, datasetId: {}, platformId: {}", uilDto.getGateId(), uilDto.getDatasetId(), uilDto.getPlatformId());
        identifierService.sendRequestUil(uilDto);
        return new ResponseEntity<>(uilDto, HttpStatus.ACCEPTED);
    }

    @PostMapping("/control/queryIdentifiers")
    public ResponseEntity<SearchWithIdentifiersRequestDto> getIdentifiers(final @RequestBody SearchWithIdentifiersRequestDto identifiersRequestDto) {
        log.info("POST on /control/queryIdentifiers on gates {} with params, identifier: {}, identifierType:{}, modeCode: {}, registrationCountryCode: {}, dangerousGoodsIndicator: {} ",
                StringUtils.join(identifiersRequestDto.getEftiGateIndicator(), ","), identifiersRequestDto.getIdentifier(),
                StringUtils.join(identifiersRequestDto.getIdentifierType(), ","), identifiersRequestDto.getModeCode(),
                identifiersRequestDto.getRegistrationCountryCode(), identifiersRequestDto.getDangerousGoodsIndicator());
        identifierService.sendIdentifierRequest(identifiersRequestDto);
        return new ResponseEntity<>(identifiersRequestDto, HttpStatus.ACCEPTED);
    }
}
