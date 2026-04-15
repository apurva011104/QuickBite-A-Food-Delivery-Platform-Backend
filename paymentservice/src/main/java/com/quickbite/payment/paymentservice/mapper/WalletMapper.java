package com.quickbite.payment.paymentservice.mapper;

import java.util.List;

import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.Wallet;

public class WalletMapper {

    public static WalletResponseDto entityToDto(Wallet wallet) {

        List<WalletStatementResponseDto> statements = wallet.getStatements()
                                                            .stream()
                                                            .map(WalletStatementMapper::entityToDto)
                                                            .toList();

        return new WalletResponseDto( wallet.getWalletId(),
                                    wallet.getCustomerId(),
                                    wallet.getBalance(),
                                    statements);
    }
}