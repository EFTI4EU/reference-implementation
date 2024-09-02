package eu.efti.identifierregistry.exception;

import java.io.Serial;

public class InvalidIdentifiersException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8858301591927593587L;

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public InvalidIdentifiersException(final String message) {
        super(message);
    }
}
