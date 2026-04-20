package com.quickbite.payment.paymentservice.service.serviceImpl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quickbite.payment.paymentservice.config.RabbitMqConfig;
import com.quickbite.payment.paymentservice.event.NotificationEvent;
import com.quickbite.payment.paymentservice.service.NotificationEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void publishPaymentNotification(NotificationEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.PAYMENT_NOTIFICATION_ROUTING_KEY,
                event
        );

        log.info("Published payment notification event type={} recipientId={} relatedId={}",
                event.getEventType(), event.getRecipientId(), event.getRelatedId());
    }
}