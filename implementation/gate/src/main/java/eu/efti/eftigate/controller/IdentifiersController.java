package eu.efti.eftigate.controller;

import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.eftigate.controller.api.IdentifiersControllerApi;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.service.ControlService;
import io.swagger.v3.oas.annotations.Parameter;
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
public class IdentifiersController implements IdentifiersControllerApi {

    private final ControlService controlService;

    @Override
    public ResponseEntity<RequestIdDto> getIdentifiers(final @RequestBody SearchWithIdentifiersRequestDto identifiersRequestDto) {
        log.info("POST on /getIdentifiers on gates {} with params, identifier: {}, identifierType:{}, modeCode: {}, registrationCountryCode: {}, dangerousGoodsIndicator: {} ",
                StringUtils.join(identifiersRequestDto.getEftiGateIndicator(), ","), identifiersRequestDto.getIdentifier(),
                StringUtils.join(identifiersRequestDto.getIdentifierType(),","), identifiersRequestDto.getModeCode(),
                identifiersRequestDto.getRegistrationCountryCode(), identifiersRequestDto.getDangerousGoodsIndicator());
        return new ResponseEntity<>(controlService.createIdentifiersControl(identifiersRequestDto), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<IdentifiersResponseDto> getIdentifiersResult(final @Parameter String requestId) {
        log.info("GET on /getIdentifiers with param requestId {}", requestId);
        return new ResponseEntity<>(controlService.getIdentifiersResponse(requestId), HttpStatus.OK);
    }
}
