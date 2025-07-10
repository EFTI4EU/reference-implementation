package eu.efti.eftigate.controller.aap;

import eu.efti.commons.dto.aap.AapUilDto;
import eu.efti.eftigate.controller.aap.api.AapControlControllerApi;
import eu.efti.eftigate.dto.RequestIdDto;
import eu.efti.eftigate.service.ControlService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
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
public class AapControlController implements AapControlControllerApi {

    private final ControlService controlService;

    @Override
    public ResponseEntity<RequestIdDto> requestUil(@Valid @RequestBody final AapUilDto uilDto) {
        log.info("POST on /aap/control/uil with params gateId: {}, datasetId: {}, platformId: {}", uilDto.getGateId(), uilDto.getDatasetId(), uilDto.getPlatformId());
        log.info("Authority: {}", uilDto.getAuthority());
        return new ResponseEntity<>(controlService.createUilControl(uilDto), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<RequestIdDto> getRequestUil(@Parameter final String requestId) {
        log.info("GET on /aap/control/uil with param requestId {}", requestId);
        return new ResponseEntity<>(controlService.getControlEntity(requestId), HttpStatus.OK);
    }
}
