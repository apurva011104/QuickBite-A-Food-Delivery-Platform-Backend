package com.quickbite.auth.authservice.service.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.quickbite.auth.authservice.service.OtpNotificationService;

@Service
public class OtpNotificationServiceImpl implements OtpNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OtpNotificationServiceImpl.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${otp.mail.from:no-reply@quickbite.local}")
    private String otpMailFrom;

    public OtpNotificationServiceImpl(ObjectProvider<JavaMailSender> javaMailSenderProvider) {
        this.javaMailSender = javaMailSenderProvider.getIfAvailable();
    }

    @Override
    @Async("otpTaskExecutor")
    public void sendRegistrationOtp(String name, String email, String otp) {
        sendEmailOtp(name, email, otp, "Verify your QuickBite account", "verify your QuickBite account");
    }

    @Override
    @Async("otpTaskExecutor")
    public void sendLoginOtp(String name, String email, String otp) {
        sendEmailOtp(name, email, otp, "QuickBite login verification", "complete your QuickBite login");
    }

    @Override
    @Async("otpTaskExecutor")
    public void sendPasswordResetOtp(String name, String email, String otp) {
        sendEmailOtp(name, email, otp, "QuickBite password reset", "reset your QuickBite password");
    }

    private void sendEmailOtp(String name, String email, String otp, String subject, String actionLabel) {
        if (javaMailSender == null || !StringUtils.hasText(mailHost)) {
            log.info("MAIL_HOST not configured. OTP for email={} is {}", email, otp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(otpMailFrom);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(buildEmailMessage(name, otp, actionLabel));
            javaMailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", email, ex);
        }
    }

    private String buildEmailMessage(String name, String otp, String actionLabel) {
        String safeName = StringUtils.hasText(name) ? name : "there";
        return "Hi " + safeName + ",\n\n"
                + "Use this OTP to " + actionLabel + ": " + otp + "\n\n"
                + "This code will expire soon. If you did not request this code, please ignore this email.\n\n"
                + "QuickBite";
    }
}
