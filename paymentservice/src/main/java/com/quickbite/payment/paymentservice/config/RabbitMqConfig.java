package com.quickbite.payment.paymentservice.config;

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
    public static final String PAYMENT_NOTIFICATION_QUEUE = "quickbite.notification.payment.queue";
    public static final String PAYMENT_NOTIFICATION_ROUTING_KEY = "notification.payment";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue paymentNotificationQueue() {
        return QueueBuilder.durable(PAYMENT_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding paymentNotificationBinding() {
        return BindingBuilder
                .bind(paymentNotificationQueue())
                .to(notificationExchange())
                .with(PAYMENT_NOTIFICATION_ROUTING_KEY);
    }
}