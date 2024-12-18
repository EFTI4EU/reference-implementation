package eu.efti.platformgatesimulator.exception;

import java.io.Serial;

public class UploadException extends Exception {

    @Serial
    private static final long serialVersionUID = -8907640121794143135L;

    public UploadException(final Exception exception) {
        super(exception);
    }
}
