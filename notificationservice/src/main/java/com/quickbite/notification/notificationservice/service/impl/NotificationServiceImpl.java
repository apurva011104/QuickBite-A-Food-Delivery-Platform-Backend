package com.quickbite.notification.notificationservice.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.notification.notificationservice.dto.requestDto.BulkNotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.MarkReadRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.NotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.responseDto.MessageResponseDto;
import com.quickbite.notification.notificationservice.dto.responseDto.NotificationResponseDto;
import com.quickbite.notification.notificationservice.entity.Notification;
import com.quickbite.notification.notificationservice.exception.ResourceNotFoundException;
import com.quickbite.notification.notificationservice.mapper.NotificationMapper;
import com.quickbite.notification.notificationservice.repository.NotificationRepository;
import com.quickbite.notification.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponseDto send(NotificationRequestDto requestDto) {
        log.info("Sending notification to recipientId: {}", requestDto.getRecipientId());

        Notification notification = notificationMapper.toEntity(requestDto);
        Notification savedNotification = notificationRepository.save(notification);

        log.info("Notification sent successfully with notificationId: {}", savedNotification.getNotificationId());
        return notificationMapper.toResponseDto(savedNotification);
    }

    @Override
    public List<NotificationResponseDto> sendBulk(BulkNotificationRequestDto requestDto) {
        log.info("Sending bulk notifications to {} recipients", requestDto.getRecipientIds().size());

        List<Notification> notifications = requestDto.getRecipientIds().stream().map(recipientId -> {
            Notification notification = new Notification();
            notification.setRecipientId(recipientId);
            notification.setType(requestDto.getType());
            notification.setTitle(requestDto.getTitle());
            notification.setMessage(requestDto.getMessage());
            notification.setChannel(requestDto.getChannel());
            notification.setRelatedId(requestDto.getRelatedId());
            notification.setRelatedType(requestDto.getRelatedType());
            return notification;
        }).toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        log.info("Bulk notifications sent successfully");
        return savedNotifications.stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto getByNotificationId(Long notificationId) {
        log.info("Fetching notification by notificationId: {}", notificationId);

        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        return notificationMapper.toResponseDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getByRecipientId(Long recipientId) {
        log.info("Fetching notifications for recipientId: {}", recipientId);

        return notificationRepository.findByRecipientId(recipientId)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadByRecipientId(Long recipientId) {
        log.info("Fetching unread notifications for recipientId: {}", recipientId);

        return notificationRepository.findByRecipientIdAndIsRead(recipientId, false)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long recipientId) {
        log.info("Fetching unread notification count for recipientId: {}", recipientId);
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public MessageResponseDto markAsRead(Long notificationId, MarkReadRequestDto requestDto) {
        log.info("Updating read status for notificationId: {}", notificationId);

        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        notification.setIsRead(requestDto.getIsRead());
        notificationRepository.save(notification);

        log.info("Notification read status updated for notificationId: {}", notificationId);
        return new MessageResponseDto("Notification read status updated successfully");
    }

    @Override
    public MessageResponseDto markAllAsRead(Long recipientId) {
        log.info("Marking all notifications as read for recipientId: {}", recipientId);

        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsRead(recipientId, false);

        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);

        log.info("All notifications marked as read for recipientId: {}", recipientId);
        return new MessageResponseDto("All notifications marked as read successfully");
    }

    @Override
    public MessageResponseDto deleteNotification(Long notificationId) {
        log.info("Deleting notificationId: {}", notificationId);

        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        notificationRepository.delete(notification);

        log.info("Notification deleted successfully for notificationId: {}", notificationId);
        return new MessageResponseDto("Notification deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getAll() {
        log.info("Fetching all notifications");

        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }
}