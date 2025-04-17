package eu.efti.platformgatesimulator.utils;

import eu.efti.commons.utils.SerializeUtils;
import eu.efti.datatools.schema.EftiSchemas;
import eu.efti.datatools.schema.XmlSchemaElement;
import eu.efti.datatools.schema.XmlUtil;
import org.w3c.dom.Document;

public class EftiSchemaUtils {
    public static eu.efti.v1.consignment.identifier.SupplyChainConsignment commonToIdentifiers(
            SerializeUtils serializeUtils,
            eu.efti.v1.consignment.common.SupplyChainConsignment common) {
        var doc = mapCommonObjectToDoc(serializeUtils, common);

        var identifiersSchema = EftiSchemas.getConsignmentIdentifierSchema();
        dropNodesNotInSchema(identifiersSchema, doc);

        // Note: this is a dirty way of fixing the namespace, but it is simple and works in our context.
        String identifiersXml = serializeUtils.mapDocToXmlString(doc).replace(
                "http://efti.eu/v1/consignment/common",
                "http://efti.eu/v1/consignment/identifier");

        return serializeUtils.mapXmlStringToJaxbObject(
                identifiersXml,
                eu.efti.v1.consignment.identifier.SupplyChainConsignment.class,
                EftiSchemas.getJavaIdentifiersSchema());
    }

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

    private static void dropNodesNotInSchema(XmlSchemaElement schema, Document doc) {
        XmlUtil.dropNodesRecursively(schema, doc.getFirstChild(), false,
                (node, maybeSchemaElement) -> maybeSchemaElement == null);
    }
}
