package com.ingroupe.efti.edeliveryapconnector.exception;

import java.io.Serial;

/**
 * Exception thrown when an error occured while sending a request
 */
public class SendRequestException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8858301591927593587L;

    /**
     * Constructor
     *
     * @param message The exception message
     * @param cause The initial cause
     */
    public SendRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public SendRequestException(final String message) {
        super(message);
    }
}
