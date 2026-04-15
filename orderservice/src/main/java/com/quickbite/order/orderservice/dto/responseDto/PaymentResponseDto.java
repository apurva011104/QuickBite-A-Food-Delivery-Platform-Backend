package com.quickbite.order.orderservice.dto.responseDto;

import java.math.BigDecimal;

import com.quickbite.order.orderservice.entity.PaymentMode;

import lombok.Data;

@Data
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMode mode;
}