package eu.efti.platformgatesimulator.utils;

import eu.efti.commons.utils.EftiSchemaUtils;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.datatools.schema.EftiSchemas;
import eu.efti.datatools.schema.XmlSchemaElement;
import eu.efti.datatools.schema.XmlUtil;
import org.w3c.dom.Document;

public class PlatformEftiSchemaUtils {
    public static eu.efti.v1.consignment.identifier.SupplyChainConsignment commonToIdentifiers(
            SerializeUtils serializeUtils,
            eu.efti.v1.consignment.common.SupplyChainConsignment common) {
        var doc = EftiSchemaUtils.mapCommonObjectToDoc(serializeUtils, common);

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

    private static void dropNodesNotInSchema(XmlSchemaElement schema, Document doc) {
        XmlUtil.dropNodesRecursively(schema, doc.getFirstChild(), false,
                (node, maybeSchemaElement) -> maybeSchemaElement == null);
    }
}
