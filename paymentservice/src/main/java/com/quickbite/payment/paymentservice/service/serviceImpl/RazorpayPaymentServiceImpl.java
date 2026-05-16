package com.quickbite.payment.paymentservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayOrderRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayWalletTopUpRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayWalletTopUpVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayOrderResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayWalletTopUpOrderResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.entity.Payment;
import com.quickbite.payment.paymentservice.entity.PaymentMode;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.event.NotificationEvent;
import com.quickbite.payment.paymentservice.exception.PaymentAlreadyProcessedException;
import com.quickbite.payment.paymentservice.exception.PaymentGatewayException;
import com.quickbite.payment.paymentservice.exception.PaymentNotFoundException;
import com.quickbite.payment.paymentservice.mapper.PaymentMapper;
import com.quickbite.payment.paymentservice.repository.PaymentRepository;
import com.quickbite.payment.paymentservice.service.NotificationEventPublisher;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.PaymentService;
import com.quickbite.payment.paymentservice.service.RazorpayPaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private static final String ORDER_CURRENCY = "INR";
    private static final String WALLET_TOP_UP_RECEIPT_PREFIX = "wallet_topup_";

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Override
    public RazorpayOrderResponseDto createRazorpayOrder(RazorpayOrderRequestDto request, UserPrincipal currentUser) {
        try {
            paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
                if (existing.getStatus() == PaymentStatus.PAID) {
                    throw new PaymentAlreadyProcessedException("Payment already completed for this order");
                }
            });

            RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

            int amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", ORDER_CURRENCY);
            orderRequest.put("receipt", "order_" + request.getOrderId());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                    .orElseGet(() -> new Payment(
                            request.getOrderId(),
                            currentUser.getUserId(),
                            request.getAmount(),
                            PaymentMode.UPI
                    ));

            payment.setCustomerId(currentUser.getUserId());
            payment.setAmount(request.getAmount());
            payment.setMode(PaymentMode.UPI);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setRazorpayOrderId(razorpayOrderId);

            paymentRepository.save(payment);

            log.info("Razorpay order created orderId={} razorpayOrderId={}",
                    request.getOrderId(), razorpayOrderId);

            return new RazorpayOrderResponseDto(
                    razorpayOrderId,
                    keyId,
                    request.getAmount(),
                    ORDER_CURRENCY
            );

        } catch (PaymentAlreadyProcessedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create Razorpay order for orderId={}", request.getOrderId(), ex);
            throw new PaymentGatewayException("Unable to create Razorpay order", ex);
        }
    }

    @Override
    public PaymentResponseDto verifyRazorpayPayment(RazorpayVerifyRequestDto request, UserPrincipal currentUser) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getCustomerId().equals(currentUser.getUserId())) {
            throw new PaymentNotFoundException("Payment not found");
        }

        if (!request.getRazorpayOrderId().equals(payment.getRazorpayOrderId())) {
            throw new PaymentGatewayException("Invalid Razorpay order ID");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentAlreadyProcessedException("Payment already completed for this order");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (!isValid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new PaymentGatewayException("Payment signature verification failed");
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setPaidAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);
            notificationEventPublisher.publishPaymentNotification(
                    new NotificationEvent(
                            "PAYMENT_SUCCESS",
                            saved.getCustomerId(),
                            "Payment Successful",
                            "Payment for order #" + saved.getOrderId() + " was successful.",
                            saved.getOrderId(),
                            "PAYMENT"
                    )
            );

            log.info("Razorpay payment verified orderId={} paymentId={}",
                    request.getOrderId(), request.getRazorpayPaymentId());

            return PaymentMapper.entityToDto(saved);

        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Razorpay verification failed orderId={}", request.getOrderId(), ex);
            throw new PaymentGatewayException("Payment verification failed", ex);
        }
    }

    @Override
    public RazorpayWalletTopUpOrderResponseDto createWalletTopUpOrder(RazorpayWalletTopUpRequestDto request,
                                                                      UserPrincipal currentUser) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentGatewayException("Wallet top-up amount must be greater than zero");
        }

        try {
            Long paymentReferenceId = -System.currentTimeMillis();
            RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

            int amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", ORDER_CURRENCY);
            orderRequest.put("receipt", WALLET_TOP_UP_RECEIPT_PREFIX + Math.abs(paymentReferenceId));

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = new Payment(paymentReferenceId, currentUser.getUserId(), request.getAmount(), PaymentMode.UPI);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setTransactionId("WALLET-TOPUP-PENDING-" + Math.abs(paymentReferenceId));
            payment.setRazorpayOrderId(razorpayOrderId);
            paymentRepository.save(payment);

            log.info("Wallet top-up Razorpay order created customerId={} paymentReferenceId={} razorpayOrderId={}",
                    currentUser.getUserId(), paymentReferenceId, razorpayOrderId);

            return new RazorpayWalletTopUpOrderResponseDto(
                    paymentReferenceId,
                    razorpayOrderId,
                    keyId,
                    request.getAmount(),
                    ORDER_CURRENCY
            );
        } catch (Exception ex) {
            log.error("Failed to create wallet top-up Razorpay order for customerId={}", currentUser.getUserId(), ex);
            throw new PaymentGatewayException("Unable to create wallet top-up Razorpay order", ex);
        }
    }

    @Override
    public WalletResponseDto verifyWalletTopUpPayment(RazorpayWalletTopUpVerifyRequestDto request,
                                                      UserPrincipal currentUser) {
        Payment payment = paymentRepository.findByOrderId(request.getPaymentReferenceId())
                .orElseThrow(() -> new PaymentNotFoundException("Wallet top-up payment not found"));

        if (!payment.getCustomerId().equals(currentUser.getUserId())) {
            throw new PaymentNotFoundException("Wallet top-up payment not found");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentAlreadyProcessedException("Wallet top-up already processed");
        }

        if (!request.getRazorpayOrderId().equals(payment.getRazorpayOrderId())) {
            throw new PaymentGatewayException("Invalid Razorpay order ID");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (!isValid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new PaymentGatewayException("Payment signature verification failed");
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setTransactionId(request.getRazorpayPaymentId());
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            WalletResponseDto wallet = paymentService.addToWallet(currentUser.getUserId(), payment.getAmount());

            log.info("Wallet top-up verified customerId={} paymentReferenceId={} paymentId={}",
                    currentUser.getUserId(), request.getPaymentReferenceId(), request.getRazorpayPaymentId());

            return wallet;
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Wallet top-up verification failed paymentReferenceId={}", request.getPaymentReferenceId(), ex);
            throw new PaymentGatewayException("Wallet top-up verification failed", ex);
        }
    }
}
