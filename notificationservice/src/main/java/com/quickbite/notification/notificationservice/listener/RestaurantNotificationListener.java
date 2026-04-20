package com.quickbite.notification.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.notification.notificationservice.config.RabbitMqConfig;
import com.quickbite.notification.notificationservice.dto.requestDto.NotificationRequestDto;
import com.quickbite.notification.notificationservice.entity.NotificationChannel;
import com.quickbite.notification.notificationservice.entity.NotificationType;
import com.quickbite.notification.notificationservice.event.NotificationEvent;
import com.quickbite.notification.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantNotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMqConfig.RESTAURANT_NOTIFICATION_QUEUE)
    public void consumeRestaurantNotification(NotificationEvent event) {
        log.info("Received restaurant notification event type={} recipientId={} relatedId={}",
                event.getEventType(), event.getRecipientId(), event.getRelatedId());

        NotificationRequestDto requestDto = new NotificationRequestDto();
        requestDto.setRecipientId(event.getRecipientId());
        requestDto.setType( "RESTAURANT_SUBMITTED_FOR_APPROVAL".equals(event.getEventType()) ? NotificationType.ADMIN : NotificationType.RESTAURANT); 
        requestDto.setTitle(event.getTitle());
        requestDto.setMessage(event.getMessage());
        requestDto.setChannel(NotificationChannel.APP);
        requestDto.setRelatedId(event.getRelatedId());
        requestDto.setRelatedType(event.getRelatedType());

        notificationService.send(requestDto);

        log.info("Restaurant notification stored for recipientId={} eventType={}",
                event.getRecipientId(), event.getEventType());
    }
}