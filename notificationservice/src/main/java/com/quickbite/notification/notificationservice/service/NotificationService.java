package com.quickbite.notification.notificationservice.service;

import java.util.List;

import com.quickbite.notification.notificationservice.dto.requestDto.BulkNotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.MarkReadRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.NotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.responseDto.MessageResponseDto;
import com.quickbite.notification.notificationservice.dto.responseDto.NotificationResponseDto;
import com.quickbite.notification.notificationservice.security.UserPrincipal;

public interface NotificationService {

    NotificationResponseDto send(NotificationRequestDto requestDto);

    List<NotificationResponseDto> sendBulk(BulkNotificationRequestDto requestDto);

    NotificationResponseDto getByNotificationId(Long notificationId, UserPrincipal currentUser);

    List<NotificationResponseDto> getByRecipientId(Long recipientId, UserPrincipal currentUser);

    List<NotificationResponseDto> getUnreadByRecipientId(Long recipientId, UserPrincipal currentUser);

    Long getUnreadCount(Long recipientId, UserPrincipal currentUser);

    MessageResponseDto markAsRead(Long notificationId, UserPrincipal currentUser, MarkReadRequestDto requestDto);

    MessageResponseDto markAllAsRead(Long recipientId, UserPrincipal currentUser);

    MessageResponseDto deleteNotification(Long notificationId, UserPrincipal currentUser);

    List<NotificationResponseDto> getAll();
}