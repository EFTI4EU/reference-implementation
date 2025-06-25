package eu.efti.eftigate.controller;

import eu.efti.commons.dto.UilDto;
import eu.efti.eftigate.controller.api.ControlControllerApi;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.service.ControlService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public ResponseEntity<RequestIdDto> requestUil(@RequestBody final UilDto uilDto, final @AuthenticationPrincipal Jwt principal) {
        if (principal != null) {
            String nationalUniqueIdentifier = principal.getClaimAsString("nationalUniqueIdentifier");
            if (StringUtils.isNotBlank(nationalUniqueIdentifier)) {
                uilDto.setNationalUniqueIdentifier(nationalUniqueIdentifier);
                log.info("POST on /control/uil with nationalUniqueIdentifier: {}", nationalUniqueIdentifier);
            }
        }
        log.info("POST on /control/uil with params gateId: {}, datasetId: {}, platformId: {}", uilDto.getGateId(), uilDto.getDatasetId(), uilDto.getPlatformId());
        return new ResponseEntity<>(controlService.createUilControl(uilDto), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<RequestIdDto> getRequestUil(@Parameter final String requestId) {
        log.info("GET on /control/uil with param requestId {}", requestId);
        return new ResponseEntity<>(controlService.getControlEntity(requestId), HttpStatus.OK);
    }
}
