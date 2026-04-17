package com.quickbite.notification.notificationservice.dto.requestDto;

import java.util.List;

import com.quickbite.notification.notificationservice.entity.NotificationChannel;
import com.quickbite.notification.notificationservice.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BulkNotificationRequestDto {

    @NotEmpty(message = "Recipient IDs are required")
    private List<Long> recipientIds;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    private Long relatedId;

    private String relatedType;
}