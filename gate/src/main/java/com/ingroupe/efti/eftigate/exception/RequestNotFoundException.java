package com.ingroupe.efti.eftigate.exception;

import java.io.Serial;

public class RequestNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8858301591927593587L;

    /**
     * Constructor
     *
     * @param message The exception message
     * @param cause   The initial cause
     */
    public RequestNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public RequestNotFoundException(final String message) {
        super(message);
    }
}
