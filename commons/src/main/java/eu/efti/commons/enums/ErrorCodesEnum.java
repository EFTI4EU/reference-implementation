package eu.efti.commons.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ErrorCodesEnum {
    XML_ERROR("Xml error"),
    UIL_GATE_MISSING("Missing parameter gateId"),

    UIL_UUID_MISSING("Missing parameter datasetId"),
    BAD_SUBSETS("Bad subsets send"),

    GATE_ID_MISSING("Missing parameter gateId"),
    GATE_ID_TOO_LONG("gateId max length is 255 characters."),
    GATE_ID_INCORRECT_FORMAT("gateId format incorrect."),

    DATASET_ID_MISSING("Missing parameter datasetId"),
    DATASET_ID_TOO_LONG("datasetId max length is 36 characters."),
    DATASET_ID_INCORRECT_FORMAT("datasetId format is incorrect."),

    PLATFORM_ID_MISSING("Missing parameter platformId"),
    PLATFORM_ID_TOO_LONG("platformId max length is 255 characters."),
    PLATFORM_ID_INCORRECT_FORMAT("platformId format incorrect."),

    REQUESTID_MISSING("Missing parameter requestId"),

    UIL_PLATFORM_MISSING("Missing parameter platformId"),

    CONTACT_MAIL_MISSING("Missing parameter email."),
    CONTACT_MAIL_INCORRECT_FORMAT("Contact mail incorrect."),
    CONTACT_MAIL_TOO_LONG("Contact mail too long."),
    CONTACT_STREET_NAME_MISSING("Missing parameter streetName."),
    CONTACT_STREET_NAME_TOO_LONG("Contact streetName too long."),
    CONTACT_BUILDING_MISSING("Missing parameter buildingNumber."),
    CONTACT_BUILDING_NUMBER_TOO_LONG("Contact building number too long."),
    CONTACT_CITY_MISSING("Missing parameter city"),
    CONTACT_CITY_TOO_LONG("Contact city too long."),
    CONTACT_ADDITIONAL_LINE_TOO_LONG("Contact additional line too long."),
    CONTACT_POSTAL_MISSING("Missing parameter postalCode."),
    CONTACT_POSTAL_CODE_TOO_LONG("Contact postal code too long."),

    IDENTIFIER_MISSING("Identifier missing."),
    IDENTIFIER_TOO_LONG("Identifier too long"),
    IDENTIFIER_INCORRECT_FORMAT("Identifier incorrect format"),
    IDENTIFIER_TYPE_INCORRECT("Identifier type is incorrect"),

    REGISTRATION_COUNTRY_INCORRECT("VehicleCountry incorrect"),
    MODE_CODE_INCORRECT_FORMAT("Mode Code Incorrect : must be one digit"),
    GATE_INDICATOR_INCORRECT("GateIndicator incorrect"),

    AP_SUBMISSION_ERROR("Error during ap submission."),
    REQUEST_BUILDING("Error while building request."),
    ID_NOT_FOUND(" Id not found."),

    PLATFORM_ERROR("Platform error"),

    DATA_NOT_FOUND("Data not found."),
    DATA_NOT_FOUND_ON_REGISTRY("Data not found on registry."),

    DEFAULT_ERROR("Error"),

    NOTE_TOO_LONG("Note max length is 255 characters.");

    private final String message;

    public static Optional<ErrorCodesEnum> fromMessage(final String errorMessage) {
        for (ErrorCodesEnum e : ErrorCodesEnum.values()) {
            if (e.getMessage().equalsIgnoreCase(errorMessage)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }
}
