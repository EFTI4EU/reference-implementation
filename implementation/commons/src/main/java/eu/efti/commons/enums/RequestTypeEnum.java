package eu.efti.commons.enums;

public enum RequestTypeEnum {
    LOCAL_UIL_SEARCH,
    EXTERNAL_UIL_SEARCH,
    EXTERNAL_ASK_UIL_SEARCH,
    LOCAL_IDENTIFIERS_SEARCH,
    EXTERNAL_IDENTIFIERS_SEARCH,
    EXTERNAL_ASK_IDENTIFIERS_SEARCH,
    NOTE_SEND,
    EXTERNAL_NOTE_SEND;

    public boolean isExternalAsk() {
        return this == EXTERNAL_ASK_UIL_SEARCH || this == EXTERNAL_ASK_IDENTIFIERS_SEARCH;
    }
}

