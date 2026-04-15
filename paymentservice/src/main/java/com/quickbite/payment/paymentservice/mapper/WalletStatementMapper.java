package com.quickbite.payment.paymentservice.mapper;

import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.WalletStatement;

public class WalletStatementMapper {

    public static WalletStatementResponseDto entityToDto(WalletStatement statement) {
        return new WalletStatementResponseDto(
                statement.getStatementId(),
                statement.getAmount(),
                statement.getType(),
                statement.getDescription(),
                statement.getCreatedAt()
        );
    }
}