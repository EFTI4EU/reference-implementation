package eu.efti.eftigate.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.List;

@FeignClient(value = "${feign-platform.name}", url = "http://efti.eu")
public interface PlatformClient {

    @GetMapping(value = "/v0/consignments/{datasetId}", consumes = MediaType.APPLICATION_XML_VALUE)
    ResponseEntity<String> sendUilQuery(URI baseUri, @PathVariable String datasetId, @RequestParam List<String> subsetId);

    @PostMapping(value = "/v0/consignments/{datasetId}/follow-up")
    ResponseEntity<Void> postConsignmentFollowUp(URI baseUri, @PathVariable String datasetId, @RequestBody String body);
}
