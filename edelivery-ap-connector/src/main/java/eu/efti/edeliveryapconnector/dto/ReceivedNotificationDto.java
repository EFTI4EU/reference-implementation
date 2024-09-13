package eu.efti.edeliveryapconnector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedNotificationDto {

    public static final String SENT_SUCCESS = "sendSuccess";
    public static final String SENT_FAILURE = "sendFailure";
    public static final String SUBMIT_MESSAGE = "submitRequest";
    public static final String MESSAGE_ID = "messageID";
    public static final String MESSAGING = "Messaging";
    public static final String PAYLOAD = "payload";

    @JsonProperty("Body")
    Map<String, Map<String, Object>> body;

    @JsonProperty("Header")
    Map<String, Object> header;

    public boolean isSentSuccess() {
        return body.containsKey(SENT_SUCCESS);
    }

    public boolean isSendFailure() {
        return body.containsKey(SENT_FAILURE);
    }

    public boolean isSubmitMessage() {
        return body.containsKey(SUBMIT_MESSAGE);
    }

    public Optional<String> getMessageId() {
        if (isSentSuccess()) {
            return Optional.of(this.body.get(SENT_SUCCESS).get(MESSAGE_ID).toString());
        } else if (isSendFailure()) {
            return Optional.of(this.body.get(SENT_FAILURE).get(MESSAGE_ID).toString());
        }
        return Optional.empty();
    }

    public Object getPayload() {
        if(this.getBody() == null || this.getBody().get(SUBMIT_MESSAGE) == null) return null;
        return this.getBody().get(SUBMIT_MESSAGE).get(PAYLOAD);
    }

    public Object getMessaging() {
        return this.getHeader().get(MESSAGING);
    }
}
