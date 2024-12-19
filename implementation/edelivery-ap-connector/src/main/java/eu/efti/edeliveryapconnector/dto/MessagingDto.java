package eu.efti.edeliveryapconnector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessagingDto {

    @JsonProperty("UserMessage")
    private UserMessage userMessage;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserMessage {
        @JsonProperty("PartyInfo")
        private PartyInfo partyInfo;
        @JsonProperty("CollaborationInfo")
        private CollaborationInfo collaborationInfo;
        @JsonProperty("MessageInfo")
        private MessageInfo messageInfo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PartyInfo {
        @JsonProperty("From")
        private From from;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class From {
        @JsonProperty("PartyId")
        private Map<String, String> partyId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PartyId {
        private String value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CollaborationInfo {
        @JsonProperty("Action")
        private String action;

        @JsonProperty("ConversationId")
        private String conversationId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MessageInfo {
        @JsonProperty("MessageId")
        private String messageId;
    }
}
