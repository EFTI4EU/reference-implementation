package eu.efti.commons.exception;


public class EftiDateTimeException extends RuntimeException {

    /**
     * Constructor
     *
     * @param message The exception message
     * @param cause   The initial cause
     */
    public EftiDateTimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param message The exception message
     */
    public EftiDateTimeException(final String message) {
        super(message);
    }
}