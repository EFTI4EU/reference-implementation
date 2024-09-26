package eu.efti.platformgatesimulator.mapper;

import eu.efti.v1.json.Consignment;
import eu.efti.v1.json.MainCarriageTransportMovement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapperUtilsTest {

    @Test
    void mapsToEdeliveryRequest() {
        // given
        eu.efti.v1.json.SaveIdentifiersRequest identifiersDto = new eu.efti.v1.json.SaveIdentifiersRequest();
        identifiersDto.setDatasetId("datasetId");
        Consignment consignment = new Consignment();
        MainCarriageTransportMovement sourceMovement = new MainCarriageTransportMovement();
        sourceMovement.setModeCode("1");
        consignment.getMainCarriageTransportMovement().add(sourceMovement);
        identifiersDto.setConsignment(consignment);

        // when
        eu.efti.v1.edelivery.SaveIdentifiersRequest result = new MapperUtils().mapToEdeliveryRequest(identifiersDto);

        // then
        assertEquals("datasetId", result.getDatasetId());
        assertEquals("1", result.getConsignment().getMainCarriageTransportMovement().get(0).getModeCode());
    }
}