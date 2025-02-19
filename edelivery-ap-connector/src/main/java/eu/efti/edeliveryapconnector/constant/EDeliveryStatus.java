package eu.efti.edeliveryapconnector.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum EDeliveryStatus {

    OK("200"),
    BAD_REQUEST("400"),
    NOT_FOUND("404"),
    INTERNAL_SERVER_ERROR("500"),
    NOT_IMPLEMENTED("501"),
    BAD_GATEWAY("502"),
    SERVICE_UNAVAILABLE("503"),
    GATEWAY_TIMEOUT("504");

    private final String code;

    public static boolean isNotFound(final String code) {
        return NOT_FOUND.name().equalsIgnoreCase(code);
    }

    public static Optional<EDeliveryStatus> fromCode(final String text) {
        for (EDeliveryStatus e : EDeliveryStatus.values()) {
            if (e.code.equalsIgnoreCase(text)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

}
