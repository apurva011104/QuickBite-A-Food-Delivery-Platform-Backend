package com.quickbite.payment.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.payment.paymentservice.entity.WalletStatement;

@Repository
public interface WalletStatementRepository extends JpaRepository<WalletStatement, Long> {
}