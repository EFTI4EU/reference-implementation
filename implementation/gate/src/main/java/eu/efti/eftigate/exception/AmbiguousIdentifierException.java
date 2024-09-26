package eu.efti.eftigate.exception;

public class AmbiguousIdentifierException extends RuntimeException{
    public AmbiguousIdentifierException(final String message) {
        super(message);
    }

    public AmbiguousIdentifierException(final Throwable cause) {
        super(cause);
    }

    public AmbiguousIdentifierException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
