package com.quickbite.delivery.deliveryservice.service;

public interface DeliveryOtpMailService {

    boolean sendDeliveryCompletionOtp(String recipientEmail, Long orderId, String otp);
}
