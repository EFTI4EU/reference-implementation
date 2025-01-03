package eu.efti.edeliveryapconnector.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApRequestDto {
    private String requestId;
    private String sender;
    private String receiver;
    private String body;
    private String eDeliveryMessageId;
    private ApConfigDto apConfig;
}
