package eu.efti.commons.constant;

import eu.efti.commons.dto.IdentifiersRequestDto;
import eu.efti.commons.dto.NotesRequestDto;
import eu.efti.commons.dto.RequestDto;
import eu.efti.commons.dto.UilRequestDto;
import eu.efti.commons.enums.RequestStatusEnum;
import eu.efti.commons.enums.RequestType;
import eu.efti.commons.enums.RequestTypeEnum;
import lombok.experimental.UtilityClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_ASK_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_NOTE_SEND;
import static eu.efti.commons.enums.RequestTypeEnum.EXTERNAL_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.LOCAL_IDENTIFIERS_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.LOCAL_UIL_SEARCH;
import static eu.efti.commons.enums.RequestTypeEnum.NOTE_SEND;

@UtilityClass
public final class EftiGateConstants {
    public static final List<RequestTypeEnum> UIL_TYPES = List.of(LOCAL_UIL_SEARCH, EXTERNAL_UIL_SEARCH, EXTERNAL_ASK_UIL_SEARCH);
    public static final List<RequestTypeEnum> IDENTIFIERS_TYPES = List.of(LOCAL_IDENTIFIERS_SEARCH, EXTERNAL_IDENTIFIERS_SEARCH, EXTERNAL_ASK_IDENTIFIERS_SEARCH);
    public static final List<RequestTypeEnum> NOTES_TYPES = List.of(NOTE_SEND, EXTERNAL_NOTE_SEND);
    public static final List<RequestTypeEnum> EXTERNAL_REQUESTS_TYPES = List.of(EXTERNAL_ASK_UIL_SEARCH, EXTERNAL_ASK_IDENTIFIERS_SEARCH);
    public static final List<RequestStatusEnum> IN_PROGRESS_STATUS = List.of(RequestStatusEnum.IN_PROGRESS, RequestStatusEnum.RESPONSE_IN_PROGRESS, RequestStatusEnum.RECEIVED);

    public static final Map<RequestType, Class<? extends RequestDto>> REQUEST_TYPE_CLASS_MAP = new EnumMap<>(Map.of(
            RequestType.UIL, UilRequestDto.class,
            RequestType.IDENTIFIER, IdentifiersRequestDto.class,
            RequestType.NOTE, NotesRequestDto.class
    ));

}
