package eu.efti.eftigate.utils;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.NonNull;

import java.util.Set;

public class StringAsObjectHttpMessageConverter extends StringHttpMessageConverter {
    private static final Set<Class<?>> support = Set.of(String.class, Object.class);

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return support.contains(clazz);
    }
}
