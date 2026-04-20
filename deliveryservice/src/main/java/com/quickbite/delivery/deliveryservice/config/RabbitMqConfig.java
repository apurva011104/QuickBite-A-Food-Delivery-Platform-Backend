package com.quickbite.delivery.deliveryservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String NOTIFICATION_EXCHANGE = "quickbite.notification.exchange";
    public static final String DELIVERY_NOTIFICATION_QUEUE = "quickbite.notification.delivery.queue";
    public static final String DELIVERY_NOTIFICATION_ROUTING_KEY = "notification.delivery";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue deliveryNotificationQueue() {
        return QueueBuilder.durable(DELIVERY_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding deliveryNotificationBinding() {
        return BindingBuilder
                .bind(deliveryNotificationQueue())
                .to(notificationExchange())
                .with(DELIVERY_NOTIFICATION_ROUTING_KEY);
    }
}