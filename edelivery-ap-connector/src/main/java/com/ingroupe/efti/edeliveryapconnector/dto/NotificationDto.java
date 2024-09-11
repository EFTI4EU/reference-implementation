package com.ingroupe.efti.edeliveryapconnector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private NotificationType notificationType;
    private String messageId;
    private NotificationContentDto content;
}
