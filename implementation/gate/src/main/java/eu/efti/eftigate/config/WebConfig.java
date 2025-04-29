package eu.efti.eftigate.config;

import eu.efti.eftigate.utils.StringAsObjectHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // We want to handle parameters of type Object and content type application/xml as plain strings so that we may
        // do xml parsing explicitly in controller method. Let's add a pass-through converter for this combination as
        // the first converter so that MappingJackson2XmlHttpMessageConverter is not used for conversion.
        var stringAsObjectHttpMessageConverter = new StringAsObjectHttpMessageConverter();
        stringAsObjectHttpMessageConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_XML));
        converters.add(0, stringAsObjectHttpMessageConverter);
    }
}
