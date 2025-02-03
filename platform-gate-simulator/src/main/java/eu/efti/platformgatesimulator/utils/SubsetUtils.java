package eu.efti.platformgatesimulator.utils;

import eu.efti.datatools.schema.SubsetUtil;
import eu.efti.datatools.schema.XmlSchemaElement;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@UtilityClass
@Log4j2
public class SubsetUtils {

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final TransformerFactory tf = TransformerFactory.newInstance();

    public Optional<String> parseBySubsets(final String xlmToParse, final List<String> subsets) {
        try {
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xlmToParse)));
            Document newDoc = SubsetUtil.filterCommonSubsets(doc, createSubsetIdList(subsets));
            return Optional.of(docToString(newDoc));
        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            log.error("Error to convert xml", e);
        }
        return Optional.empty();
    }

    private String docToString(final Document doc) throws TransformerException {
        try {
            final DOMSource domSource = new DOMSource(doc);
            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException e) {
            log.error("Error when try to convert Document to String: " + e);
            throw new TransformerException(e.getMessage());
        }
    }

    private Set<XmlSchemaElement.SubsetId> createSubsetIdList(final List<String> stringSubsetId) {
        final Set<XmlSchemaElement.SubsetId> subsetIdSet = new HashSet<>();
        stringSubsetId.forEach(s -> subsetIdSet.add(new XmlSchemaElement.SubsetId(s)));

        return subsetIdSet;
    }
}
