package com.quickbite.order.orderservice.service.serviceImpl;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.quickbite.order.orderservice.config.RabbitMqConfig;
import com.quickbite.order.orderservice.event.NotificationEvent;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldPublishNotificationEvent() {
        NotificationEventPublisherImpl publisher = new NotificationEventPublisherImpl(rabbitTemplate);
        NotificationEvent event = new NotificationEvent("ORDER_PLACED", 1L, "Placed", "Placed", 100L, "ORDER");

        publisher.publishOrderNotification(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.ORDER_NOTIFICATION_ROUTING_KEY,
                event
        );
    }
}
