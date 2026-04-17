package com.quickbite.notification.notificationservice.mapper;

import org.springframework.stereotype.Component;

import com.quickbite.notification.notificationservice.dto.requestDto.NotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.responseDto.NotificationResponseDto;
import com.quickbite.notification.notificationservice.entity.Notification;

@Component
public class NotificationMapper {

    public Notification toEntity(NotificationRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipientId(dto.getRecipientId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setChannel(dto.getChannel());
        notification.setRelatedId(dto.getRelatedId());
        notification.setRelatedType(dto.getRelatedType());

        return notification;
    }

    public NotificationResponseDto toResponseDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setNotificationId(notification.getNotificationId());
        dto.setRecipientId(notification.getRecipientId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setChannel(notification.getChannel());
        dto.setRelatedId(notification.getRelatedId());
        dto.setRelatedType(notification.getRelatedType());
        dto.setIsRead(notification.getIsRead());
        dto.setSentAt(notification.getSentAt());

        return dto;
    }
}