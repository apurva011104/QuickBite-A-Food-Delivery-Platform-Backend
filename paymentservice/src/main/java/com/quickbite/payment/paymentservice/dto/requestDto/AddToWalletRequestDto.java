package com.quickbite.payment.paymentservice.dto.requestDto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToWalletRequestDto {

    @NotNull
    private BigDecimal amount;
}