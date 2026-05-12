package com.quickbite.payment.paymentservice.dto.responseDto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayOrderResponseDto {

    private String razorpayOrderId;
    private String keyId;
    private BigDecimal amount;
    private String currency;
}