package com.quickbite.payment.paymentservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

class ConfigTest {

    @Test
    void rabbitMqConfigCreatesExpectedBeans() {
        RabbitMqConfig config = new RabbitMqConfig();

        DirectExchange exchange = config.notificationExchange();
        Queue queue = config.paymentNotificationQueue();
        Binding binding = config.paymentNotificationBinding();

        assertEquals(RabbitMqConfig.NOTIFICATION_EXCHANGE, exchange.getName());
        assertEquals(RabbitMqConfig.PAYMENT_NOTIFICATION_QUEUE, queue.getName());
        assertEquals(RabbitMqConfig.PAYMENT_NOTIFICATION_ROUTING_KEY, binding.getRoutingKey());
    }

    @Test
    void rabbitTemplateConfigCreatesJsonConverter() {
        RabbitTemplateConfig config = new RabbitTemplateConfig();
        Jackson2JsonMessageConverter converter = config.jackson2JsonMessageConverter();

        assertNotNull(converter);
    }
}
