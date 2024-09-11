package com.ingroupe.efti.commons.constant;

import com.ingroupe.efti.commons.dto.IdentifiersRequestDto;
import com.ingroupe.efti.commons.dto.NotesRequestDto;
import com.ingroupe.efti.commons.dto.RequestDto;
import com.ingroupe.efti.commons.dto.UilRequestDto;
import com.ingroupe.efti.commons.enums.EDeliveryAction;
import com.ingroupe.efti.commons.enums.RequestStatusEnum;
import com.ingroupe.efti.commons.enums.RequestType;
import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import lombok.experimental.UtilityClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.ingroupe.efti.commons.enums.EDeliveryAction.FORWARD_UIL;
import static com.ingroupe.efti.commons.enums.EDeliveryAction.GET_IDENTIFIERS;
import static com.ingroupe.efti.commons.enums.EDeliveryAction.GET_UIL;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_METADATA_SEARCH;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_METADATA_SEARCH;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_NOTE_SEND;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.EXTERNAL_UIL_SEARCH;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.LOCAL_METADATA_SEARCH;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.LOCAL_UIL_SEARCH;
import static com.ingroupe.efti.commons.enums.RequestTypeEnum.NOTE_SEND;

@UtilityClass
public final class EftiGateConstants {
    public static final List<RequestTypeEnum> UIL_TYPES = List.of(LOCAL_UIL_SEARCH, EXTERNAL_UIL_SEARCH, EXTERNAL_ASK_UIL_SEARCH);
    public static final List<RequestTypeEnum> IDENTIFIERS_TYPES = List.of(LOCAL_METADATA_SEARCH, EXTERNAL_METADATA_SEARCH, EXTERNAL_ASK_METADATA_SEARCH);
    public static final List<RequestTypeEnum> NOTES_TYPES = List.of(NOTE_SEND, EXTERNAL_NOTE_SEND);
    public static final List<RequestTypeEnum> EXTERNAL_REQUESTS_TYPES = List.of(EXTERNAL_ASK_UIL_SEARCH, EXTERNAL_ASK_METADATA_SEARCH);
    public static final List<EDeliveryAction> UIL_ACTIONS = List.of(FORWARD_UIL, GET_UIL);
    public static final List<EDeliveryAction> IDENTIFIERS_ACTIONS = List.of(GET_IDENTIFIERS);
    public static final List<RequestStatusEnum> IN_PROGRESS_STATUS = List.of(RequestStatusEnum.IN_PROGRESS, RequestStatusEnum.RESPONSE_IN_PROGRESS, RequestStatusEnum.RECEIVED);

    public static final EnumMap<RequestType, Class<? extends RequestDto>> REQUEST_TYPE_CLASS_MAP = new EnumMap<>(Map.of(
            RequestType.UIL, UilRequestDto.class,
            RequestType.IDENTIFIER, IdentifiersRequestDto.class,
            RequestType.NOTE, NotesRequestDto.class
    ));

}
