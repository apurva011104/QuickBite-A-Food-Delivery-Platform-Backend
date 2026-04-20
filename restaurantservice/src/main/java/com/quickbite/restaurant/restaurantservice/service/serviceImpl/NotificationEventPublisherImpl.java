package com.quickbite.restaurant.restaurantservice.service.serviceImpl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.quickbite.restaurant.restaurantservice.config.RabbitMqConfig;
import com.quickbite.restaurant.restaurantservice.event.NotificationEvent;
import com.quickbite.restaurant.restaurantservice.service.NotificationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishRestaurantNotification(NotificationEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.RESTAURANT_NOTIFICATION_ROUTING_KEY,
                event
        );

        log.info("Published restaurant notification event type={} recipientId={} relatedId={}",
                event.getEventType(), event.getRecipientId(), event.getRelatedId());
    }
}