package com.quickbite.restaurant.restaurantservice.service;

import com.quickbite.restaurant.restaurantservice.event.NotificationEvent;

public interface NotificationEventPublisher {
    void publishRestaurantNotification(NotificationEvent event);
}