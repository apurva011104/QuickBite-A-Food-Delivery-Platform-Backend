package com.quickbite.order.orderservice.entity;

public enum OrderStatus {
    PLACED,
    PAYMENT_PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    REJECTED,
    CANCELLED;
}
