package eu.efti.eftigate.service;

import eu.efti.eftigate.dto.RabbitRequestDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GateIntegrationService {
    private final DomibusIntegrationService domibusIntegrationService;

    void handle(final RabbitRequestDto rabbitRequestDto) {
        domibusIntegrationService.trySendDomibus(rabbitRequestDto, rabbitRequestDto.getControl().getRequestType(), rabbitRequestDto.getGateIdDest());
    }
}
