package com.quickbite.payment.paymentservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayOrderRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayOrderResponseDto;
import com.quickbite.payment.paymentservice.entity.Payment;
import com.quickbite.payment.paymentservice.entity.PaymentMode;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.exception.PaymentAlreadyProcessedException;
import com.quickbite.payment.paymentservice.exception.PaymentGatewayException;
import com.quickbite.payment.paymentservice.exception.PaymentNotFoundException;
import com.quickbite.payment.paymentservice.mapper.PaymentMapper;
import com.quickbite.payment.paymentservice.repository.PaymentRepository;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
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

    private final PaymentRepository paymentRepository;

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
            orderRequest.put("currency", "INR");
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
                    "INR"
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

            log.info("Razorpay payment verified orderId={} paymentId={}",
                    request.getOrderId(), request.getRazorpayPaymentId());

            return PaymentMapper.entityToDto(saved);

        } catch (Exception ex) {
            log.error("Razorpay verification failed orderId={}", request.getOrderId(), ex);
            throw new PaymentGatewayException("Payment verification failed", ex);
        }
    }
}