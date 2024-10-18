package eu.efti.eftigate.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import eu.efti.commons.utils.SerializeUtils;
import eu.efti.eftigate.mapper.MapperUtils;
import eu.efti.identifiersregistry.IdentifiersMapper;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public abstract class AbstractServiceTest {

    @Spy
    public final MapperUtils mapperUtils = new MapperUtils(createModelMapper(), new IdentifiersMapper(createModelMapper()));

    public final SerializeUtils serializeUtils = new SerializeUtils(objectMapper(), xmlMapper());

    private ModelMapper createModelMapper() {
        return new ModelMapper();
    }

    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.coercionConfigDefaults().setCoercion(CoercionInputShape.String, CoercionAction.AsEmpty)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty);
        return objectMapper;
    }

    public XmlMapper xmlMapper() {
        final XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.registerModule(new JakartaXmlBindAnnotationModule());
        return xmlMapper;
    }
}
