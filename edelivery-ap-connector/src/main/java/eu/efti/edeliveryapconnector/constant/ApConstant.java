package eu.efti.edeliveryapconnector.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApConstant {

    public static final String PARTY_TYPE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
    public static final String ORIGINAL_SENDER_PROPERTY_KEY = "originalSender";
    public static final String FINAL_RECIPIENT_PROPERTY_KEY = "finalRecipient";
    public static final String ORIGINAL_SENDER_PROPERTY_VALUE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1";
    public static final String FINAL_RECIPIENT_PROPERTY_VALUE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4";
    public static final String TEXT_PLAIN = "text/plain; charset=UTF-8";
    public static final String PAYLOAD_HREF = "cid:message";
    public static final String MIME_TYPE = "MimeType";
    public static final String PARTY_FROM_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator";
    public static final String PARTY_TO_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder";
    public static final String SERVICE_TYPE = "tc1";
    public static final String SERVICE_VALUE = "bdx:noprocess";
}
