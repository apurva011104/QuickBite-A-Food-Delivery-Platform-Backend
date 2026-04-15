package com.quickbite.payment.paymentservice.dto.requestDto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletPaymentRequestDto {

    @NotNull
    private Long orderId;

    @NotNull
    private BigDecimal amount;
}