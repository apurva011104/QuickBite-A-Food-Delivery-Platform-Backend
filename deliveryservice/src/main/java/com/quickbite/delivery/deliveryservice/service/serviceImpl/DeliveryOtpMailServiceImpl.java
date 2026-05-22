package com.quickbite.delivery.deliveryservice.service.serviceImpl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.quickbite.delivery.deliveryservice.service.DeliveryOtpMailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryOtpMailServiceImpl implements DeliveryOtpMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${delivery.otp.mail.from:}")
    private String fromAddress;

    @Override
    public boolean sendDeliveryCompletionOtp(String recipientEmail, Long orderId, String otp) {
        if (!isMailConfigured() || recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Skipping delivery OTP email for orderId={} because mail config or recipient is missing", orderId);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipientEmail);
            message.setSubject("QuickBite delivery verification OTP for order #" + orderId);
            message.setText(buildMessageBody(orderId, otp));
            mailSender.send(message);
            log.info("Delivery OTP email sent for orderId={} to {}", orderId, recipientEmail);
            return true;
        } catch (MailException ex) {
            log.error("Failed to send delivery OTP email for orderId={} to {}", orderId, recipientEmail, ex);
            return false;
        }
    }

    private boolean isMailConfigured() {
        return mailHost != null
                && !mailHost.isBlank()
                && fromAddress != null
                && !fromAddress.isBlank();
    }

    private String buildMessageBody(Long orderId, String otp) {
        return "Your QuickBite delivery verification OTP for order #"
                + orderId
                + " is "
                + otp
                + ". Share this code with your delivery partner only when your order reaches you.";
    }
}
