package com.quickbite.order.orderservice.dto.requestDto;

import java.math.BigDecimal;

import com.quickbite.order.orderservice.entity.PaymentMode;

import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long orderId;
    private BigDecimal amount;
    private PaymentMode mode;
}