package com.quickbite.delivery.deliveryservice.service.serviceImpl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quickbite.delivery.deliveryservice.config.RabbitMqConfig;
import com.quickbite.delivery.deliveryservice.event.NotificationEvent;
import com.quickbite.delivery.deliveryservice.service.NotificationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void publishDeliveryNotification(NotificationEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.DELIVERY_NOTIFICATION_ROUTING_KEY,
                event
        );

        log.info("Published delivery notification event type={} recipientId={} relatedId={}",
                event.getEventType(), event.getRecipientId(), event.getRelatedId());
    }
}