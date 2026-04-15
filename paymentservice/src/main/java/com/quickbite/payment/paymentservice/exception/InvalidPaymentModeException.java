package com.quickbite.payment.paymentservice.exception;

public class InvalidPaymentModeException extends RuntimeException {
    public InvalidPaymentModeException(String message) {
        super(message);
    }
}