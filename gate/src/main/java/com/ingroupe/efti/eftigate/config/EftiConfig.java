package com.ingroupe.efti.eftigate.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.AbstractConverter;
import org.modelmapper.AbstractProvider;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.Provider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

@Configuration
@ComponentScan(basePackages = "com.ingroupe.efti")
@Slf4j
public class EftiConfig {

    public static final String OFFSET_PATTERN = "+HH:MM";
    public static final String NO_OFFSET_TEXT = "+00:00";
    public static final String UTC = "UTC";
    public static final String INVALID_DATE_FORMAT = "invalid date format {}";

    @Bean(name = "modelMapper")
    public ModelMapper modelMapper() {
        final ModelMapper modelMapper = new ModelMapper();
        final Provider<LocalDateTime> localDateProvider = new AbstractProvider<>() {
            @Override
            public LocalDateTime get() {
                return LocalDateTime.now();
            }
        };

        final Converter<String, LocalDateTime> toLocalDateTime = new AbstractConverter<>() {
            @Override
            protected LocalDateTime convert(final String source) {
                try {
                    return getLocalDateTime(source);
                } catch (final DateTimeParseException e) {
                    log.error(INVALID_DATE_FORMAT, source);
                    return null;
                }
            }
        };

        final Converter<LocalDateTime, String> toStringLocalDateTime = new AbstractConverter<>() {
            @Override
            protected String convert(final LocalDateTime source) {
                try {
                    if (source != null) {
                        final DateTimeFormatter dateTimeFormatter = getDateTimeFormatter();
                        return source.atZone(ZoneId.of(UTC)).format(dateTimeFormatter);
                    }
                } catch (final DateTimeParseException e) {
                    log.error(INVALID_DATE_FORMAT, source);
                    return null;
                }
                return null;
            }
        };

        final Converter<String, OffsetDateTime> toOffsetDateTime = new AbstractConverter<>() {
            @Override
            protected OffsetDateTime convert(final String source) {
                try {
                    return StringUtils.isNotBlank(source) ? OffsetDateTime.parse(source) : null;
                } catch (final DateTimeParseException e) {
                    log.error(INVALID_DATE_FORMAT, source);
                    return null;
                }
            }
        };

        final Converter<OffsetDateTime, String> toStringOffsetDateTime = new AbstractConverter<>() {
            @Override
            protected String convert(final OffsetDateTime source) {
                try {
                    if (source != null) {
                        final DateTimeFormatter dateTimeFormatter = getDateTimeFormatter();
                        return source.toZonedDateTime().format(dateTimeFormatter);
                    }
                } catch (final DateTimeParseException e) {
                    log.error(INVALID_DATE_FORMAT, source);
                    return null;
                }
                return null;
            }
        };
        modelMapper.createTypeMap(String.class, LocalDateTime.class);
        modelMapper.createTypeMap(String.class, OffsetDateTime.class);
        modelMapper.addConverter(toLocalDateTime);
        modelMapper.addConverter(toOffsetDateTime);
        modelMapper.addConverter(toStringLocalDateTime);
        modelMapper.addConverter(toStringOffsetDateTime);
        modelMapper.getTypeMap(String.class, LocalDateTime.class).setProvider(localDateProvider);
        return modelMapper;
    }

    private static DateTimeFormatter getDateTimeFormatter() {
        return new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .appendOffset(OFFSET_PATTERN, NO_OFFSET_TEXT)
                .toFormatter();
    }

    private static LocalDateTime getLocalDateTime(final String source) {
        if (StringUtils.isNotBlank(source)) {
            return OffsetDateTime.parse(source).toLocalDateTime();
        }
        return null;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.featuresToDisable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE); // make sure timezones are not being converted to GMT when parsing json
    }

    @Bean
    @ConfigurationProperties(prefix = "gate")
    public GateProperties gateProperties() {
        return new GateProperties();
    }

    @Bean(name = "objectMapper")
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.coercionConfigDefaults().setCoercion(CoercionInputShape.String, CoercionAction.AsEmpty)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty);
        return objectMapper;
    }

    @Bean(name = "xmlMapper")
    public XmlMapper xmlMapper() {
        final XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.registerModule(new JakartaXmlBindAnnotationModule());
        return xmlMapper;
    }
}
