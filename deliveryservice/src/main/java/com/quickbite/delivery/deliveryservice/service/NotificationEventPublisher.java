package com.quickbite.delivery.deliveryservice.service;

import com.quickbite.delivery.deliveryservice.event.NotificationEvent;

public interface NotificationEventPublisher {
    void publishDeliveryNotification(NotificationEvent event);
}