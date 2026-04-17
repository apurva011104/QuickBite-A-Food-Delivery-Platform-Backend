package com.quickbite.notification.notificationservice.service;

import java.util.List;

import com.quickbite.notification.notificationservice.dto.requestDto.BulkNotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.MarkReadRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.NotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.responseDto.MessageResponseDto;
import com.quickbite.notification.notificationservice.dto.responseDto.NotificationResponseDto;

public interface NotificationService {

    NotificationResponseDto send(NotificationRequestDto requestDto);

    List<NotificationResponseDto> sendBulk(BulkNotificationRequestDto requestDto);

    NotificationResponseDto getByNotificationId(Long notificationId);

    List<NotificationResponseDto> getByRecipientId(Long recipientId);

    List<NotificationResponseDto> getUnreadByRecipientId(Long recipientId);

    Long getUnreadCount(Long recipientId);

    MessageResponseDto markAsRead(Long notificationId, MarkReadRequestDto requestDto);

    MessageResponseDto markAllAsRead(Long recipientId);

    MessageResponseDto deleteNotification(Long notificationId);

    List<NotificationResponseDto> getAll();
}