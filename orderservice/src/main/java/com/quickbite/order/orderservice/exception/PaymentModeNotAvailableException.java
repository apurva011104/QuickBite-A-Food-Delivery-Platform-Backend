package com.quickbite.order.orderservice.exception;

public class PaymentModeNotAvailableException extends RuntimeException {
    public PaymentModeNotAvailableException(String message) {
        super(message);
    }
}