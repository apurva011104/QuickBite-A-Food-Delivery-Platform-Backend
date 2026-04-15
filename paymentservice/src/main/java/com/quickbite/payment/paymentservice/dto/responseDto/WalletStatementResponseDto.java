package com.quickbite.payment.paymentservice.dto.responseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.quickbite.payment.paymentservice.entity.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletStatementResponseDto {

    private Long statementId;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDateTime createdAt;
}