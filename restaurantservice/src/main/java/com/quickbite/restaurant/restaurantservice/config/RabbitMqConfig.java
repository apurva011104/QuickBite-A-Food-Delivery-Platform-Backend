package com.quickbite.restaurant.restaurantservice.config;

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
    public static final String RESTAURANT_NOTIFICATION_QUEUE = "quickbite.notification.restaurant.queue";
    public static final String RESTAURANT_NOTIFICATION_ROUTING_KEY = "notification.restaurant";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue restaurantNotificationQueue() {
        return QueueBuilder.durable(RESTAURANT_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding restaurantNotificationBinding() {
        return BindingBuilder
                .bind(restaurantNotificationQueue())
                .to(notificationExchange())
                .with(RESTAURANT_NOTIFICATION_ROUTING_KEY);
    }
}