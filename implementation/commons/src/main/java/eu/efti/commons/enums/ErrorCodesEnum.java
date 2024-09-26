package eu.efti.commons.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodesEnum {
    UIL_GATE_MISSING("Missing parameter eFTIGateUrl"),
    UIL_GATE_TOO_LONG("Gate max length is 255 characters."),
    UIL_GATE_INCORRECT_FORMAT("Gate format incorrect."),

    UIL_UUID_MISSING("Missing parameter eFTIDataUuid"),
    UIL_PLATFORM_TOO_LONG("Platform max length is 255 characters."),
    UIL_PLATFORM_INCORRECT_FORMAT("Platform format incorrect."),

    UIL_PLATFORM_MISSING("Missing parameter eFTIPlatformUrl"),
    UIL_UUID_TOO_LONG("Uuid max length is 36 characters."),
    UIL_UUID_INCORRECT_FORMAT("Uuid format incorrect."),

    AUTHORITY_MISSING("Authority missing."),
    AUTHORITY_COUNTRY_MISSING("Authority country missing."),
    AUTHORITY_COUNTRY_TOO_LONG("Authority country too long."),
    AUTHORITY_COUNTRY_UNKNOWN("Authority country unknown."),
    AUTHORITY_LEGAL_CONTACT_MISSING("Authority legal contact missing."),
    AUTHORITY_WORKING_CONTACT_MISSING("Authority working contact missing."),
    AUTHORITY_IS_EMERGENCY_MISSING("Authority is emergency missing."),
    AUTHORITY_NAME_MISSING("Authority name missing."),
    AUTHORITY_NAME_TOO_LONG("Authority name too long."),
    AUTHORITY_NATIONAL_IDENTIFIER_MISSING("Authority national identifier missing."),
    AUTHORITY_NATIONAL_IDENTIFIER_TOO_LONG("Authority national identifier too long."),

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

    VEHICLE_ID_MISSING("VehicleId missing."),
    VEHICLE_ID_TOO_LONG("VehicleId too long"),
    VEHICLE_ID_INCORRECT_FORMAT("VehicleId incorrect format"),

    VEHICLE_COUNTRY_INCORRECT("VehicleCountry incorrect"),
    TRANSPORT_MODE_INCORRECT("TransportMode Incorrect"),
    GATE_INDICATOR_INCORRECT("GateIndicator incorrect"),

    AP_SUBMISSION_ERROR("Error during ap submission."),
    REQUEST_BUILDING("Error while building request."),
    UUID_NOT_FOUND(" Uuid not found."),

    PLATFORM_ERROR("Platform error"),

    DATA_NOT_FOUND("Data not found."),
    DATA_NOT_FOUND_ON_REGISTRY("Data not found on registry."),

    DEFAULT_ERROR("Error"),

    NOTE_TOO_LONG("Note max length is 255 characters.");

    private final String message;
}
