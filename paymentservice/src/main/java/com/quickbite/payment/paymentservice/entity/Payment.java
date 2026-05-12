package com.quickbite.payment.paymentservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode mode;

    @Column(unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String currency = "INR";

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    @Column(unique = true)
    private String razorpayOrderId;
    
    @Column(unique = true)
    private String razorpayPaymentId;

    public Payment(Long orderId, Long customerId, BigDecimal amount, PaymentMode mode) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.mode = mode;
        this.status = PaymentStatus.PENDING;
        this.currency = "INR";
    }
}