package com.quickbite.payment.paymentservice.dto.responseDto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletResponseDto {

    private Long walletId;
    private Long customerId;
    private BigDecimal balance;
    private List<WalletStatementResponseDto> statements;
}