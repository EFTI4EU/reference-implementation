package eu.efti.commons.utils;

import org.w3c.dom.Document;

public class EftiSchemaUtils {
    public static Document mapCommonObjectToDoc(
            SerializeUtils serializeUtils,
            eu.efti.v1.consignment.common.SupplyChainConsignment consignmentCommon) {
        return serializeUtils.mapJaxbObjectToDoc(consignmentCommon, eu.efti.v1.consignment.common.SupplyChainConsignment.class,
                "consignment", "http://efti.eu/v1/consignment/common");
    }

    public static Document mapIdentifiersObjectToDoc(
            SerializeUtils serializeUtils,
            eu.efti.v1.consignment.identifier.SupplyChainConsignment consignmentIdentifiers) {
        return serializeUtils.mapJaxbObjectToDoc(consignmentIdentifiers, eu.efti.v1.consignment.identifier.SupplyChainConsignment.class,
                "consignment", "http://efti.eu/v1/consignment/identifier");
    }
}
