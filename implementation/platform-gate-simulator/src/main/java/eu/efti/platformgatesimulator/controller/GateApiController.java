package eu.efti.platformgatesimulator.controller;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.EftiSchemaUtils;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.controller.api.V0Api;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gate-api")
public class GateApiController implements V0Api {
    private final ReaderService readerService;

    private final GateProperties gateProperties;

    private final SerializeUtils serializeUtils;

    @Override
    public ResponseEntity<Object> getConsignmentSubsets(String datasetId, Set<String> subsetId) {
        try {
            final SupplyChainConsignment supplyChainConsignment = readerService.readFromFile(gateProperties.getCdaPath() + datasetId, subsetId.stream().toList());
            if (supplyChainConsignment != null) {
                var xml = serializeUtils.mapDocToXmlString(EftiSchemaUtils.mapCommonObjectToDoc(serializeUtils, supplyChainConsignment));
                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(xml);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (TechnicalException | IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> postConsignmentFollowup(String datasetId, String body) {
        log.info("note \"{}\" received for datasetId {}", body, datasetId);
        return ResponseEntity.ok().build();
    }
}
