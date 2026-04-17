package com.quickbite.notification.notificationservice.dto.responseDto;

import java.time.LocalDateTime;

import com.quickbite.notification.notificationservice.entity.NotificationChannel;
import com.quickbite.notification.notificationservice.entity.NotificationType;

import lombok.Data;

@Data
public class NotificationResponseDto {

    private Long notificationId;
    private Long recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationChannel channel;
    private Long relatedId;
    private String relatedType;
    private Boolean isRead;
    private LocalDateTime sentAt;
}