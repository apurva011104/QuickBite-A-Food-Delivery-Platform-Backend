package com.quickbite.order.orderservice.config;

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
    public static final String ORDER_NOTIFICATION_QUEUE = "quickbite.notification.order.queue";
    public static final String ORDER_NOTIFICATION_ROUTING_KEY = "notification.order";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue orderNotificationQueue() {
        return QueueBuilder.durable(ORDER_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding orderNotificationBinding() {
        return BindingBuilder
                .bind(orderNotificationQueue())
                .to(notificationExchange())
                .with(ORDER_NOTIFICATION_ROUTING_KEY);
    }
}