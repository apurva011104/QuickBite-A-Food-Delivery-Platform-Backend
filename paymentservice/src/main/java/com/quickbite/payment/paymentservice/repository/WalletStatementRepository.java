package com.quickbite.payment.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.payment.paymentservice.entity.WalletStatement;

public interface WalletStatementRepository extends JpaRepository<WalletStatement, Long> {
}