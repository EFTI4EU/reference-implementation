package eu.efti.platformgatesimulator.controller;

import eu.efti.commons.exception.TechnicalException;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.platformgatesimulator.config.GateProperties;
import eu.efti.platformgatesimulator.service.ReaderService;
import eu.efti.v1.consignment.common.ObjectFactory;
import eu.efti.v1.consignment.common.SupplyChainConsignment;
import jakarta.xml.bind.JAXBElement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/v0/consignments")
public class PlatformApiForGateController {

    private final ReaderService readerService;

    private final GateProperties gateProperties;

    private final SerializeUtils serializeUtils;

    private final ObjectFactory objectFactory = new ObjectFactory();


    @GetMapping("/{datasetId}")
    public ResponseEntity<Object> getConsignmentsSubsets(@PathVariable("datasetId") String datasetId, @RequestParam List<String> subsetId) {
        try {
            final SupplyChainConsignment supplyChainConsignment = readerService.readFromFile(gateProperties.getCdaPath() + "/" + datasetId, subsetId);
            if (supplyChainConsignment != null) {
                JAXBElement<SupplyChainConsignment> consignment = objectFactory.createConsignment(supplyChainConsignment);
                String xml = serializeUtils.mapJaxbObjectToXmlString(consignment, SupplyChainConsignment.class);
                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(xml);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (TechnicalException | IOException e) {
            log.error("Error reading consignments from file for datasetId: {}", datasetId, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{datasetId}/follow-up")
    public ResponseEntity<Void> postConsignmentFollowup(@PathVariable("datasetId") String datasetId, @RequestBody String body) {
        log.info("note \"{}\" received for datasetId {}", body, datasetId);
        return ResponseEntity.noContent().build();
    }
}
