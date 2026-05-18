package com.quickbite.auth.authservice.service.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    public void sendSignupOtp(String name, String email, String otp) {
        sendEmailOtp(name, email, otp);
    }

    private void sendEmailOtp(String name, String email, String otp) {
        if (javaMailSender == null || !StringUtils.hasText(mailHost)) {
            log.info("MAIL_HOST not configured. Signup OTP for email={} is {}", email, otp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(otpMailFrom);
            message.setTo(email);
            message.setSubject("Verify your QuickBite account");
            message.setText(buildEmailMessage(name, otp));
            javaMailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", email, ex);
            throw new RuntimeException("Unable to send verification email right now");
        }
    }

    private String buildEmailMessage(String name, String otp) {
        String safeName = StringUtils.hasText(name) ? name : "there";
        return "Hi " + safeName + ",\n\n"
                + "Use this OTP to verify your QuickBite account: " + otp + "\n\n"
                + "This code will expire soon. If you did not try to sign up, please ignore this email.\n\n"
                + "QuickBite";
    }
}
