package com.ingroupe.efti.commons.enums;

public enum RequestTypeEnum {
    LOCAL_UIL_SEARCH,
    EXTERNAL_UIL_SEARCH,
    EXTERNAL_ASK_UIL_SEARCH,
    LOCAL_METADATA_SEARCH,
    EXTERNAL_METADATA_SEARCH,
    EXTERNAL_ASK_METADATA_SEARCH,
    NOTE_SEND,
    EXTERNAL_NOTE_SEND;

    public boolean isExternalAsk() {
        return this == EXTERNAL_ASK_UIL_SEARCH || this == EXTERNAL_ASK_METADATA_SEARCH;
    }
}

