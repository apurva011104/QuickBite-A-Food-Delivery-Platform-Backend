package com.quickbite.payment.paymentservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import com.quickbite.payment.paymentservice.service.PaymentService;
import com.quickbite.payment.paymentservice.util.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto request, String token) {

        Long customerId = jwtUtil.extractUserId(token);

        log.info("Processing payment for customerId={}, orderId={}, mode={}",
                customerId, request.getOrderId(), request.getMode());

        Payment payment = PaymentMapper.dtoToEntity(request, customerId);

        switch (request.getMode()) {

            case CARD:
            case UPI:
                payment.setStatus(PaymentStatus.PAID);
                payment.setTransactionId("TXN-" + System.currentTimeMillis());
                payment.setPaidAt(LocalDateTime.now());

                log.info("Payment successful via {} for orderId={}", request.getMode(), request.getOrderId());
                break;

            case COD:
                payment.setStatus(PaymentStatus.PENDING);

                log.info("COD selected for orderId={}, marking as PENDING", request.getOrderId());
                break;

            case WALLET:
                log.info("Redirecting to wallet payment for orderId={}", request.getOrderId());
                return payFromWallet(token, request.getOrderId(), request.getAmount());

            default:
                log.error("Invalid payment mode received: {}", request.getMode());
                throw new InvalidPaymentModeException("Invalid payment mode");
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Payment saved with paymentId={}", saved.getPaymentId());

        return PaymentMapper.entityToDto(saved);
    }

    @Transactional
    @Override
    public PaymentResponseDto payFromWallet(String token, Long orderId, BigDecimal amount) {

        Long customerId = jwtUtil.extractUserId(token);

        log.info("Wallet payment initiated for customerId={}, orderId={}, amount={}",
                customerId, orderId, amount);

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.error("Wallet not found for customerId={}", customerId);
                    return new WalletNotFoundException("Wallet not found");
                });

        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for customerId={}, available={}, required={}",
                    customerId, wallet.getBalance(), amount);
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));

        WalletStatement statement = new WalletStatement(
                amount,
                TransactionType.DEBIT,
                "Payment for order " + orderId
        );

        wallet.addStatement(statement);
        walletRepository.save(wallet);

        log.info("Wallet debited successfully for customerId={}, new balance={}",
                customerId, wallet.getBalance());

        Payment payment = new Payment(orderId, customerId, amount, PaymentMode.WALLET);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId("WALLET-" + System.currentTimeMillis());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        log.info("Wallet payment successful, paymentId={}", saved.getPaymentId());

        return PaymentMapper.entityToDto(saved);
    }

    @Override
    public WalletResponseDto addToWallet(String token, BigDecimal amount) {

        Long customerId = jwtUtil.extractUserId(token);

        log.info("Adding money to wallet for customerId={}, amount={}", customerId, amount);

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Creating new wallet for customerId={}", customerId);
                    return new Wallet(customerId);
                });

        wallet.setBalance(wallet.getBalance().add(amount));

        WalletStatement statement = new WalletStatement(
                amount,
                TransactionType.CREDIT,
                "Wallet top-up"
        );

        wallet.addStatement(statement);

        Wallet saved = walletRepository.save(wallet);

        log.info("Wallet updated for customerId={}, new balance={}", customerId, saved.getBalance());

        return WalletMapper.entityToDto(saved);
    }

    
    @Override
    public PaymentResponseDto getPaymentByOrder(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        return PaymentMapper.entityToDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByCustomer(String token) {

        Long customerId = jwtUtil.extractUserId(token);

        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(PaymentMapper::entityToDto)
                .toList();
    }

    @Override
    public PaymentResponseDto updatePaymentStatus(Long paymentId, PaymentStatus status) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        payment.setStatus(status);

        if (status == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }

        if (status == PaymentStatus.REFUNDED) {
            payment.setRefundedAt(LocalDateTime.now());
        }

        return PaymentMapper.entityToDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponseDto refundPayment(Long orderId) {

        log.info("Refund initiated for orderId={}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderId={}", orderId);
                    return new PaymentNotFoundException("Payment not found");
                });

        if (payment.getStatus() != PaymentStatus.PAID) {
            log.warn("Refund attempted on non-paid orderId={}, status={}",
                    orderId, payment.getStatus());
            throw new PaymentAlreadyProcessedException("Cannot refund unpaid order");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        if (payment.getMode() == PaymentMode.WALLET) {

            Wallet wallet = walletRepository.findByCustomerId(payment.getCustomerId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

            wallet.setBalance(wallet.getBalance().add(payment.getAmount()));

            WalletStatement statement = new WalletStatement(
                    payment.getAmount(),
                    TransactionType.CREDIT,
                    "Refund for order " + orderId
            );

            wallet.addStatement(statement);
            walletRepository.save(wallet);

            log.info("Refund credited to wallet for customerId={}", payment.getCustomerId());
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Refund completed for orderId={}, paymentId={}", orderId, saved.getPaymentId());

        return PaymentMapper.entityToDto(saved);
    }

        
    @Override
    public WalletResponseDto getWallet(String token) {

        Long customerId = jwtUtil.extractUserId(token);

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        return WalletMapper.entityToDto(wallet);
    }


    @Override
    public BigDecimal getWalletBalance(String token) {

        Long customerId = jwtUtil.extractUserId(token);

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        return wallet.getBalance();
    }

    @Override
    public List<WalletStatementResponseDto> getWalletStatements(String token) {

        Long customerId = jwtUtil.extractUserId(token);

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        return wallet.getStatements()
                .stream()
                .map(WalletStatementMapper::entityToDto)
                .toList();
    }
}