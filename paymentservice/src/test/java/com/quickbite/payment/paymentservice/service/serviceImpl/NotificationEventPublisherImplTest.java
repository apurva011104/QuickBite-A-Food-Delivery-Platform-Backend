package com.quickbite.payment.paymentservice.service.serviceImpl;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.payment.paymentservice.config.RabbitMqConfig;
import com.quickbite.payment.paymentservice.event.NotificationEvent;

class NotificationEventPublisherImplTest {

    @Test
    void publishPaymentNotificationUsesExpectedExchangeAndRoutingKey() {
        RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
        NotificationEventPublisherImpl publisher = new NotificationEventPublisherImpl();
        ReflectionTestUtils.setField(publisher, "rabbitTemplate", rabbitTemplate);
        NotificationEvent event = new NotificationEvent("PAYMENT_SUCCESS", 1L, "Payment Successful",
                "Order paid", 10L, "PAYMENT");

        publisher.publishPaymentNotification(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.PAYMENT_NOTIFICATION_ROUTING_KEY,
                event
        );
    }
}
