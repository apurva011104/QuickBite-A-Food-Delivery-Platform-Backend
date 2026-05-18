package com.quickbite.auth.authservice.service;

public interface OtpNotificationService {

    void sendRegistrationOtp(String name, String email, String otp);

    void sendLoginOtp(String name, String email, String otp);

    void sendPasswordResetOtp(String name, String email, String otp);
}
