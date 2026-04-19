package com.quickbite.payment.paymentservice.dto.requestDto;

import java.math.BigDecimal;

import com.quickbite.payment.paymentservice.entity.PaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull
    private Long orderId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @NotNull
    private PaymentMode mode;
}