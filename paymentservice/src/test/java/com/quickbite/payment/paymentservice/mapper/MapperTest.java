package com.quickbite.payment.paymentservice.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.Payment;
import com.quickbite.payment.paymentservice.entity.PaymentMode;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.entity.TransactionType;
import com.quickbite.payment.paymentservice.entity.Wallet;
import com.quickbite.payment.paymentservice.entity.WalletStatement;

class MapperTest {

    @Test
    void paymentMapperConvertsBetweenDtoAndEntity() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(700L);
        request.setAmount(new BigDecimal("250.00"));
        request.setMode(PaymentMode.UPI);

        Payment payment = PaymentMapper.dtoToEntity(request, 70L);
        payment.setPaymentId(1L);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("txn-1");

        PaymentResponseDto response = PaymentMapper.entityToDto(payment);

        assertEquals(700L, payment.getOrderId());
        assertEquals(70L, payment.getCustomerId());
        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertEquals("txn-1", response.getTransactionId());
    }

    @Test
    void walletMapperIncludesMappedStatements() {
        Wallet wallet = new Wallet(80L);
        wallet.setWalletId(8L);
        wallet.setBalance(new BigDecimal("400.00"));
        WalletStatement statement = new WalletStatement(new BigDecimal("50.00"), TransactionType.CREDIT, "Wallet top-up");
        statement.setStatementId(9L);
        wallet.addStatement(statement);

        WalletResponseDto response = WalletMapper.entityToDto(wallet);
        WalletStatementResponseDto mappedStatement = response.getStatements().get(0);

        assertEquals(8L, response.getWalletId());
        assertEquals(new BigDecimal("400.00"), response.getBalance());
        assertEquals(9L, mappedStatement.getStatementId());
        assertEquals(TransactionType.CREDIT, mappedStatement.getType());
    }
}
