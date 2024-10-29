package eu.efti.eftigate.controller;

import eu.efti.commons.dto.IdentifiersResponseDto;
import eu.efti.commons.dto.SearchWithIdentifiersRequestDto;
import eu.efti.eftigate.controller.api.IdentifiersControllerApi;
import eu.efti.eftigate.dto.RequestUuidDto;
import eu.efti.eftigate.service.ControlService;
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
public class IdentifiersController implements IdentifiersControllerApi {

    private final ControlService controlService;

    @Override
    public ResponseEntity<RequestUuidDto> getIdentifiers(final @RequestBody SearchWithIdentifiersRequestDto identifiersRequestDto) {
        log.info("POST on /getIdentifiers with param vehicleID {}", identifiersRequestDto.getIdentifier());
        return new ResponseEntity<>(controlService.createIdentifiersControl(identifiersRequestDto), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<IdentifiersResponseDto> getIdentifiersResult(final @Parameter String requestUuid) {
        log.info("GET on /getIdentifiers with param requestUuid {}", requestUuid);
        return new ResponseEntity<>(controlService.getIdentifiersResponse(requestUuid), HttpStatus.OK);
    }
}
