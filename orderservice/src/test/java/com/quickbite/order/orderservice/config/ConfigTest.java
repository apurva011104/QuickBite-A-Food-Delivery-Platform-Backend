package com.quickbite.order.orderservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.web.client.RestTemplate;

class ConfigTest {

    @Test
    void rabbitMqConfigShouldCreateBeans() {
        RabbitMqConfig config = new RabbitMqConfig();

        DirectExchange exchange = config.notificationExchange();
        Queue queue = config.orderNotificationQueue();
        Binding binding = config.orderNotificationBinding();

        assertThat(exchange.getName()).isEqualTo(RabbitMqConfig.NOTIFICATION_EXCHANGE);
        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.ORDER_NOTIFICATION_QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.ORDER_NOTIFICATION_ROUTING_KEY);
    }

    @Test
    void templateAndRestConfigShouldCreateBeans() {
        RabbitTemplateConfig rabbitTemplateConfig = new RabbitTemplateConfig();
        RestTemplateConfig restTemplateConfig = new RestTemplateConfig();

        Jackson2JsonMessageConverter converter = rabbitTemplateConfig.jackson2JsonMessageConverter();
        RestTemplate restTemplate = restTemplateConfig.restTemplate();

        assertThat(converter).isNotNull();
        assertThat(restTemplate).isNotNull();
    }
}
