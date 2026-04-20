package com.quickbite.payment.paymentservice.service;

import com.quickbite.payment.paymentservice.event.NotificationEvent;

public interface NotificationEventPublisher {
    void publishPaymentNotification(NotificationEvent event);
}