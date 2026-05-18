package com.quickbite.auth.authservice.service;

public interface OtpNotificationService {

    void sendSignupOtp(String name, String email, String otp);
}
