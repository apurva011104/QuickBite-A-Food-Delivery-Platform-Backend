package com.quickbite.order.orderservice.service;

import com.quickbite.order.orderservice.event.NotificationEvent;

public interface NotificationEventPublisher {
    void publishOrderNotification(NotificationEvent event);
}