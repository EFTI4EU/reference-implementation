package eu.efti.commons.enums;

import java.util.Optional;

public enum StatusEnum {
    PENDING,
    COMPLETE,
    ERROR,
    TIMEOUT;

    public static Optional<StatusEnum> fromString(String text) {
        for (StatusEnum e : StatusEnum.values()) {
            if (e.name().equalsIgnoreCase(text)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

}
