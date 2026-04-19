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
import com.quickbite.notification.notificationservice.exception.UnauthorizedActionException;
import com.quickbite.notification.notificationservice.mapper.NotificationMapper;
import com.quickbite.notification.notificationservice.repository.NotificationRepository;
import com.quickbite.notification.notificationservice.security.UserPrincipal;
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
        log.info("Sending notification to recipientId={}", requestDto.getRecipientId());

        Notification notification = notificationMapper.toEntity(requestDto);
        Notification savedNotification = notificationRepository.save(notification);

        log.info("Notification stored successfully notificationId={}", savedNotification.getNotificationId());

        // later: email sender / SMS sender / websocket push / RabbitMQ consumer flow
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

        return savedNotifications.stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto getByNotificationId(Long notificationId, UserPrincipal currentUser) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        validateRecipientAccess(notification.getRecipientId(), currentUser);
        return notificationMapper.toResponseDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getByRecipientId(Long recipientId, UserPrincipal currentUser) {
        validateRecipientAccess(recipientId, currentUser);

        return notificationRepository.findByRecipientId(recipientId)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadByRecipientId(Long recipientId, UserPrincipal currentUser) {
        validateRecipientAccess(recipientId, currentUser);

        return notificationRepository.findByRecipientIdAndIsRead(recipientId, false)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long recipientId, UserPrincipal currentUser) {
        validateRecipientAccess(recipientId, currentUser);
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public MessageResponseDto markAsRead(Long notificationId, UserPrincipal currentUser, MarkReadRequestDto requestDto) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        validateRecipientAccess(notification.getRecipientId(), currentUser);

        notification.setIsRead(requestDto.getIsRead());
        notificationRepository.save(notification);

        return new MessageResponseDto("Notification read status updated successfully");
    }

    @Override
    public MessageResponseDto markAllAsRead(Long recipientId, UserPrincipal currentUser) {
        validateRecipientAccess(recipientId, currentUser);

        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsRead(recipientId, false);
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);

        return new MessageResponseDto("All notifications marked as read successfully");
    }

    @Override
    public MessageResponseDto deleteNotification(Long notificationId, UserPrincipal currentUser) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        validateRecipientAccess(notification.getRecipientId(), currentUser);
        notificationRepository.delete(notification);

        return new MessageResponseDto("Notification deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    private void validateRecipientAccess(Long recipientId, UserPrincipal currentUser) {
        if (!recipientId.equals(currentUser.getUserId()) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedActionException("You are not allowed to access these notifications");
        }
    }
}