package com.quickbite.order.orderservice.entity;

public enum OrderStatus {
    PLACED,
    PAYMENT_PENDING,
    CONFIRMED,
    PREPARING,
    PICKED_UP,
    DELIVERED,
    CANCELLED;
}
