package com.quickbite.payment.paymentservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.quickbite.payment.paymentservice.event.NotificationEvent;
import com.quickbite.payment.paymentservice.exception.InsufficientBalanceException;
import com.quickbite.payment.paymentservice.exception.InvalidPaymentModeException;
import com.quickbite.payment.paymentservice.exception.PaymentAlreadyProcessedException;
import com.quickbite.payment.paymentservice.exception.PaymentNotFoundException;
import com.quickbite.payment.paymentservice.exception.WalletNotFoundException;
import com.quickbite.payment.paymentservice.mapper.PaymentMapper;
import com.quickbite.payment.paymentservice.mapper.WalletMapper;
import com.quickbite.payment.paymentservice.mapper.WalletStatementMapper;
import com.quickbite.payment.paymentservice.repository.PaymentRepository;
import com.quickbite.payment.paymentservice.repository.WalletRepository;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.NotificationEventPublisher;
import com.quickbite.payment.paymentservice.service.PaymentService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private NotificationEventPublisher notificationEventPublisher;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              WalletRepository walletRepository) {
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request, UserPrincipal currentUser) {
        Long customerId = currentUser.getUserId();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentModeException("Payment amount must be greater than zero");
        }

        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PAID || existing.getStatus() == PaymentStatus.PENDING) {
                throw new PaymentAlreadyProcessedException("Payment already exists for this order");
            }
        });

        log.info("Processing payment customerId={} orderId={} mode={}",
                customerId, request.getOrderId(), request.getMode());

        Payment payment = PaymentMapper.dtoToEntity(request, customerId);

        switch (request.getMode()) {
            case CARD, UPI -> {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTransactionId(request.getMode().name() + "-PENDING-" + UUID.randomUUID());

                Payment saved = paymentRepository.save(payment);
                log.info("Online payment initialized for orderId={} mode={}, awaiting gateway verification",
                        request.getOrderId(), request.getMode());
                return PaymentMapper.entityToDto(saved);
            }

            case COD -> {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTransactionId("COD-" + UUID.randomUUID());

                Payment saved = paymentRepository.save(payment);
                log.info("COD selected for orderId={}, payment kept PENDING", request.getOrderId());
                return PaymentMapper.entityToDto(saved);
            }

            case WALLET -> {
                return payFromWallet(customerId, request.getOrderId(), request.getAmount());
            }

            default -> throw new InvalidPaymentModeException("Invalid payment mode");
        }
    }

    @Override
    public PaymentResponseDto getPaymentByOrder(Long orderId, UserPrincipal currentUser) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if ("CUSTOMER".equals(currentUser.getRole()) && !payment.getCustomerId().equals(currentUser.getUserId())) {
            throw new PaymentNotFoundException("Payment not found");
        }

        return PaymentMapper.entityToDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(PaymentMapper::entityToDto)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDto updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        payment.setStatus(status);

        if (status == PaymentStatus.PAID && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        if (status == PaymentStatus.REFUNDED) {
            payment.setRefundedAt(LocalDateTime.now());
        }

        Payment updated = paymentRepository.save(payment);
        log.info("Payment status updated paymentId={} status={}", paymentId, status);

        return PaymentMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Long orderId, UserPrincipal currentUser) {
        log.info("Refund initiated for orderId={} by role={}", orderId, currentUser.getRole());

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if ("CUSTOMER".equals(currentUser.getRole()) && !payment.getCustomerId().equals(currentUser.getUserId())) {
            throw new PaymentNotFoundException("Payment not found");
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new PaymentAlreadyProcessedException("Cannot refund unpaid or already refunded payment");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        if (payment.getMode() == PaymentMode.WALLET) {
            Wallet wallet = walletRepository.findByCustomerId(payment.getCustomerId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

            wallet.setBalance(wallet.getBalance().add(payment.getAmount()));
            wallet.addStatement(new WalletStatement(
                    payment.getAmount(),
                    TransactionType.CREDIT,
                    "Refund for order " + orderId
            ));

            walletRepository.save(wallet);
            log.info("Refund credited to wallet for customerId={}", payment.getCustomerId());
        }

        Payment saved = paymentRepository.save(payment);

        notificationEventPublisher.publishPaymentNotification(
                new NotificationEvent(
                        "PAYMENT_REFUNDED",
                        saved.getCustomerId(),
                        "Payment Refunded",
                        "Refund for order #" + saved.getOrderId() + " has been processed.",
                        saved.getOrderId(),
                        "PAYMENT"
                )
        );
        
        log.info("Refund completed paymentId={} orderId={}", saved.getPaymentId(), orderId);

        return PaymentMapper.entityToDto(saved);
    }

    @Override
    public WalletResponseDto getWallet(Long customerId) {
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> walletRepository.save(new Wallet(customerId)));

        return WalletMapper.entityToDto(wallet);
    }

    @Override
    public BigDecimal getWalletBalance(Long customerId) {
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> walletRepository.save(new Wallet(customerId)));

        return wallet.getBalance();
    }

    @Override
    @Transactional
    public WalletResponseDto addToWallet(Long customerId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentModeException("Wallet top-up amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Creating new wallet for customerId={}", customerId);
                    return new Wallet(customerId);
                });

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.addStatement(new WalletStatement(amount, TransactionType.CREDIT, "Wallet top-up"));

        Wallet saved = walletRepository.save(wallet);

        notificationEventPublisher.publishPaymentNotification(
                new NotificationEvent(
                        "WALLET_TOPUP_SUCCESS",
                        customerId,
                        "Wallet Top-up Successful",
                        "Your wallet has been credited with Rs. " + amount + ".",
                        saved.getWalletId(),
                        "WALLET"
                )
        );

        log.info("Wallet topped up customerId={} amount={} balance={}", customerId, amount, saved.getBalance());

        return WalletMapper.entityToDto(saved);
    }

    @Override
    @Transactional
    public PaymentResponseDto payFromWallet(Long customerId, Long orderId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentModeException("Wallet payment amount must be greater than zero");
        }

        paymentRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PAID || existing.getStatus() == PaymentStatus.PENDING) {
                throw new PaymentAlreadyProcessedException("Payment already exists for this order");
            }
        });

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.addStatement(new WalletStatement(amount, TransactionType.DEBIT, "Payment for order " + orderId));
        walletRepository.save(wallet);

        Payment payment = new Payment(orderId, customerId, amount, PaymentMode.WALLET);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("WALLET-" + UUID.randomUUID());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        notificationEventPublisher.publishPaymentNotification(
                new NotificationEvent(
                        "PAYMENT_SUCCESS",
                        saved.getCustomerId(),
                        "Wallet Payment Successful",
                        "Wallet payment for order #" + saved.getOrderId() + " was successful.",
                        saved.getOrderId(),
                        "PAYMENT"
                )
        );

        log.info("Wallet payment successful paymentId={} orderId={} customerId={}",
                saved.getPaymentId(), orderId, customerId);

        return PaymentMapper.entityToDto(saved);
    }

    @Override
    public List<WalletStatementResponseDto> getWalletStatements(Long customerId) {
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> walletRepository.save(new Wallet(customerId)));

        return wallet.getStatements()
                .stream()
                .map(WalletStatementMapper::entityToDto)
                .toList();
    }
}
