package com.ingroupe.platform.platformgatesimulator.exception;

import java.io.Serial;

public class UuidFileNotFoundException extends Exception {

    @Serial
    private static final long serialVersionUID = -8907640121794143135L;

    public UuidFileNotFoundException(final String message) {
        super(message);
    }
}
