package eu.efti.platformgatesimulator.controller.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@FeignClient(value = "${feign-gate.name}", url = "${feign-gate.url}")
public interface CallGateFeign {

    @PutMapping(value = "/v0/consignments/{datasetId}", consumes = APPLICATION_XML_VALUE, produces = APPLICATION_XML_VALUE)
    ResponseEntity<String> sendIdentifiers(@RequestHeader Map<String, String> headers, @PathVariable String datasetId, @RequestBody String body);
}
