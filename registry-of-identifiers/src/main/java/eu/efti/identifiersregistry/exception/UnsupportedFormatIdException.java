package eu.efti.identifiersregistry.exception;

import java.io.Serial;

public class UnsupportedFormatIdException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -4015158393328203802L;

    public UnsupportedFormatIdException(final String message) {
        super(message);
    }
}
