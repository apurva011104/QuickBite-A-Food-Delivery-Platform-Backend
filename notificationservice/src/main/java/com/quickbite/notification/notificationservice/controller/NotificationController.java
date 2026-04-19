package com.quickbite.notification.notificationservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.notification.notificationservice.dto.requestDto.BulkNotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.MarkReadRequestDto;
import com.quickbite.notification.notificationservice.dto.requestDto.NotificationRequestDto;
import com.quickbite.notification.notificationservice.dto.responseDto.MessageResponseDto;
import com.quickbite.notification.notificationservice.dto.responseDto.NotificationResponseDto;
import com.quickbite.notification.notificationservice.security.UserPrincipal;
import com.quickbite.notification.notificationservice.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDto> send(@Valid @RequestBody NotificationRequestDto requestDto) {
        log.info("API HIT - Send notification to recipientId={}", requestDto.getRecipientId());
        return new ResponseEntity<>(notificationService.send(requestDto), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponseDto>> sendBulk(
            @Valid @RequestBody BulkNotificationRequestDto requestDto) {
        log.info("API HIT - Send bulk notifications");
        return new ResponseEntity<>(notificationService.sendBulk(requestDto), HttpStatus.CREATED);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDto> getByNotificationId(@PathVariable Long notificationId,
                                                                       Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getByNotificationId(notificationId, currentUser));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationResponseDto>> getByRecipientId(@PathVariable Long recipientId,
                                                                          Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getByRecipientId(recipientId, currentUser));
    }

    @GetMapping("/recipient/{recipientId}/unread")
    public ResponseEntity<List<NotificationResponseDto>> getUnreadByRecipientId(@PathVariable Long recipientId,
                                                                                Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getUnreadByRecipientId(recipientId, currentUser));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long recipientId,
                                               Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId, currentUser));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<MessageResponseDto> markAsRead(@PathVariable Long notificationId,
                                                         @Valid @RequestBody MarkReadRequestDto requestDto,
                                                         Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, currentUser, requestDto));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<MessageResponseDto> markAllAsRead(@PathVariable Long recipientId,
                                                            Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.markAllAsRead(recipientId, currentUser));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<MessageResponseDto> deleteNotification(@PathVariable Long notificationId,
                                                                 Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.deleteNotification(notificationId, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }
}