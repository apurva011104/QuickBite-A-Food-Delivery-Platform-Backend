package com.quickbite.order.orderservice.service.serviceImpl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.quickbite.order.orderservice.config.RabbitMqConfig;
import com.quickbite.order.orderservice.event.NotificationEvent;
import com.quickbite.order.orderservice.service.NotificationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderNotification(NotificationEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.ORDER_NOTIFICATION_ROUTING_KEY,
                event
        );

        log.info("Published notification event type={} recipientId={} relatedId={}",
                event.getEventType(), event.getRecipientId(), event.getRelatedId());
    }
}