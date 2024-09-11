package com.ingroupe.efti.metadataregistry.exception;

import java.io.Serial;

public class InvalidMetadataException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8858301591927593587L;

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public InvalidMetadataException(final String message) {
        super(message);
    }
}
