package com.quickbite.payment.paymentservice.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.quickbite.payment.paymentservice.repository.PaymentRepository;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.NotificationEventPublisher;
import com.quickbite.payment.paymentservice.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private RazorpayPaymentServiceImpl razorpayPaymentService;
    private UserPrincipal currentUser;

    @BeforeEach
    void setUp() {
        razorpayPaymentService = new RazorpayPaymentServiceImpl(
                paymentRepository,
                paymentService,
                notificationEventPublisher
        );
        ReflectionTestUtils.setField(razorpayPaymentService, "keyId", "rzp_test_key");
        ReflectionTestUtils.setField(razorpayPaymentService, "keySecret", "rzp_test_secret");
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        currentUser = new UserPrincipal(33L, "customer@quickbite.com", "CUSTOMER");
    }

    @Test
    void createRazorpayOrderBuildsPendingPaymentAndReturnsCheckoutPayload() throws Exception {
        RazorpayOrderRequestDto request = new RazorpayOrderRequestDto();
        request.setOrderId(400L);
        request.setAmount(new BigDecimal("499.99"));

        when(paymentRepository.findByOrderId(400L)).thenReturn(Optional.empty());

        try (MockedConstruction<RazorpayClient> mockedClient = mockConstruction(RazorpayClient.class, (mock, context) -> {
            OrderClient orderClient = mock(OrderClient.class);
            Order order = mock(Order.class);
            when(order.get("id")).thenReturn("order_RZP_123");
            when(orderClient.create(any(JSONObject.class))).thenReturn(order);
            mock.orders = orderClient;
        })) {
            RazorpayOrderResponseDto response = razorpayPaymentService.createRazorpayOrder(request, currentUser);

            assertEquals("order_RZP_123", response.getRazorpayOrderId());
            assertEquals("rzp_test_key", response.getKeyId());
            assertEquals(new BigDecimal("499.99"), response.getAmount());
            assertEquals("INR", response.getCurrency());
            assertEquals(1, mockedClient.constructed().size());
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Test
    void createRazorpayOrderRejectsAlreadyPaidOrder() {
        Payment existing = new Payment(401L, currentUser.getUserId(), new BigDecimal("20.00"), PaymentMode.UPI);
        existing.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(401L)).thenReturn(Optional.of(existing));

        RazorpayOrderRequestDto request = new RazorpayOrderRequestDto();
        request.setOrderId(401L);
        request.setAmount(new BigDecimal("20.00"));

        assertThrows(PaymentAlreadyProcessedException.class,
                () -> razorpayPaymentService.createRazorpayOrder(request, currentUser));
    }

    @Test
    void createRazorpayOrderWrapsGatewayFailures() {
        RazorpayOrderRequestDto request = new RazorpayOrderRequestDto();
        request.setOrderId(402L);
        request.setAmount(new BigDecimal("10.00"));
        when(paymentRepository.findByOrderId(402L)).thenReturn(Optional.empty());

        try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class, (mock, context) -> {
            OrderClient orderClient = mock(OrderClient.class);
            when(orderClient.create(any(JSONObject.class))).thenThrow(new RuntimeException("boom"));
            mock.orders = orderClient;
        })) {
            PaymentGatewayException exception = assertThrows(PaymentGatewayException.class,
                    () -> razorpayPaymentService.createRazorpayOrder(request, currentUser));
            assertEquals("Unable to create Razorpay order", exception.getMessage());
        }
    }

    @Test
    void createRazorpayOrderConvertsAmountToPaise() throws Exception {
        RazorpayOrderRequestDto request = new RazorpayOrderRequestDto();
        request.setOrderId(408L);
        request.setAmount(new BigDecimal("10.50"));
        when(paymentRepository.findByOrderId(408L)).thenReturn(Optional.empty());

        try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class, (mock, context) -> {
            OrderClient orderClient = mock(OrderClient.class);
            when(orderClient.create(any(JSONObject.class))).thenAnswer(invocation -> {
                JSONObject payload = invocation.getArgument(0);
                assertEquals(1050, payload.getInt("amount"));
                assertEquals("INR", payload.getString("currency"));
                assertTrue(payload.getString("receipt").contains("408"));
                Order order = mock(Order.class);
                when(order.get("id")).thenReturn("order_RZP_408");
                return order;
            });
            mock.orders = orderClient;
        })) {
            RazorpayOrderResponseDto response = razorpayPaymentService.createRazorpayOrder(request, currentUser);
            assertEquals("order_RZP_408", response.getRazorpayOrderId());
        }
    }

    @Test
    void verifyRazorpayPaymentMarksPaymentPaidOnValidSignature() {
        Payment payment = new Payment(403L, currentUser.getUserId(), new BigDecimal("90.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_RZP_403");
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(403L)).thenReturn(Optional.of(payment));

        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(403L);
        request.setRazorpayOrderId("order_RZP_403");
        request.setRazorpayPaymentId("pay_403");
        request.setRazorpaySignature("signature_403");

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any(String.class))).thenReturn(true);

            PaymentResponseDto response = razorpayPaymentService.verifyRazorpayPayment(request, currentUser);

            assertEquals(PaymentStatus.PAID, response.getStatus());
            assertEquals("pay_403", response.getTransactionId());
            assertEquals("pay_403", response.getRazorpayPaymentId());
            assertNotNull(response.getPaidAt());
            verify(notificationEventPublisher).publishPaymentNotification(any(NotificationEvent.class));
        }
    }

    @Test
    void verifyRazorpayPaymentRejectsAlreadyPaidOrder() {
        Payment payment = new Payment(4031L, currentUser.getUserId(), new BigDecimal("90.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_RZP_4031");
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(4031L)).thenReturn(Optional.of(payment));

        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(4031L);
        request.setRazorpayOrderId("order_RZP_4031");
        request.setRazorpayPaymentId("pay_4031");
        request.setRazorpaySignature("signature_4031");

        assertThrows(PaymentAlreadyProcessedException.class,
                () -> razorpayPaymentService.verifyRazorpayPayment(request, currentUser));
    }

    @Test
    void verifyRazorpayPaymentRejectsDifferentCustomer() {
        Payment payment = new Payment(404L, 77L, new BigDecimal("50.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_RZP_404");
        when(paymentRepository.findByOrderId(404L)).thenReturn(Optional.of(payment));

        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(404L);
        request.setRazorpayOrderId("order_RZP_404");
        request.setRazorpayPaymentId("pay_404");
        request.setRazorpaySignature("signature_404");

        assertThrows(PaymentNotFoundException.class,
                () -> razorpayPaymentService.verifyRazorpayPayment(request, currentUser));
    }

    @Test
    void verifyRazorpayPaymentRejectsMismatchedOrderId() {
        Payment payment = new Payment(405L, currentUser.getUserId(), new BigDecimal("50.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_expected");
        when(paymentRepository.findByOrderId(405L)).thenReturn(Optional.of(payment));

        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(405L);
        request.setRazorpayOrderId("order_other");
        request.setRazorpayPaymentId("pay_405");
        request.setRazorpaySignature("signature_405");

        PaymentGatewayException exception = assertThrows(PaymentGatewayException.class,
                () -> razorpayPaymentService.verifyRazorpayPayment(request, currentUser));
        assertEquals("Invalid Razorpay order ID", exception.getMessage());
    }

    @Test
    void verifyRazorpayPaymentMarksPaymentFailedWhenSignatureIsInvalid() {
        Payment payment = new Payment(406L, currentUser.getUserId(), new BigDecimal("50.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_RZP_406");
        when(paymentRepository.findByOrderId(406L)).thenReturn(Optional.of(payment));

        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(406L);
        request.setRazorpayOrderId("order_RZP_406");
        request.setRazorpayPaymentId("pay_406");
        request.setRazorpaySignature("signature_406");

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any(String.class))).thenReturn(false);

            PaymentGatewayException exception = assertThrows(PaymentGatewayException.class,
                    () -> razorpayPaymentService.verifyRazorpayPayment(request, currentUser));
            assertEquals("Payment signature verification failed", exception.getMessage());
            assertEquals(PaymentStatus.FAILED, payment.getStatus());
            verify(paymentRepository).save(payment);
            verify(notificationEventPublisher, never()).publishPaymentNotification(any(NotificationEvent.class));
        }
    }

    @Test
    void verifyRazorpayPaymentThrowsWhenPaymentIsMissing() {
        when(paymentRepository.findByOrderId(407L)).thenReturn(Optional.empty());

        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(407L);
        request.setRazorpayOrderId("order_RZP_407");
        request.setRazorpayPaymentId("pay_407");
        request.setRazorpaySignature("signature_407");

        assertThrows(PaymentNotFoundException.class,
                () -> razorpayPaymentService.verifyRazorpayPayment(request, currentUser));
    }

    @Test
    void createWalletTopUpOrderCreatesPendingPaymentAndReturnsCheckoutPayload() throws Exception {
        RazorpayWalletTopUpRequestDto request = new RazorpayWalletTopUpRequestDto();
        request.setAmount(new BigDecimal("250.00"));

        try (MockedConstruction<RazorpayClient> ignored = mockConstruction(RazorpayClient.class, (mock, context) -> {
            OrderClient orderClient = mock(OrderClient.class);
            Order order = mock(Order.class);
            when(order.get("id")).thenReturn("order_wallet_1");
            when(orderClient.create(any(JSONObject.class))).thenReturn(order);
            mock.orders = orderClient;
        })) {
            RazorpayWalletTopUpOrderResponseDto response =
                    razorpayPaymentService.createWalletTopUpOrder(request, currentUser);

            assertTrue(response.getPaymentReferenceId() < 0);
            assertEquals("order_wallet_1", response.getRazorpayOrderId());
            assertEquals("rzp_test_key", response.getKeyId());
            assertEquals(new BigDecimal("250.00"), response.getAmount());
            assertEquals("INR", response.getCurrency());
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Test
    void verifyWalletTopUpPaymentCreditsWalletOnValidSignature() {
        Payment payment = new Payment(-501L, currentUser.getUserId(), new BigDecimal("300.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_wallet_501");
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(-501L)).thenReturn(Optional.of(payment));
        when(paymentService.addToWallet(currentUser.getUserId(), new BigDecimal("300.00")))
                .thenReturn(new WalletResponseDto(12L, currentUser.getUserId(), new BigDecimal("800.00"), List.of()));

        RazorpayWalletTopUpVerifyRequestDto request = new RazorpayWalletTopUpVerifyRequestDto();
        request.setPaymentReferenceId(-501L);
        request.setRazorpayOrderId("order_wallet_501");
        request.setRazorpayPaymentId("pay_wallet_501");
        request.setRazorpaySignature("sig_wallet_501");

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any(String.class))).thenReturn(true);

            WalletResponseDto response = razorpayPaymentService.verifyWalletTopUpPayment(request, currentUser);

            assertEquals(new BigDecimal("800.00"), response.getBalance());
            assertEquals(PaymentStatus.PAID, payment.getStatus());
            assertEquals("pay_wallet_501", payment.getTransactionId());
            verify(paymentService).addToWallet(currentUser.getUserId(), new BigDecimal("300.00"));
        }
    }

    @Test
    void verifyWalletTopUpPaymentRejectsAlreadyProcessedPayment() {
        Payment payment = new Payment(-502L, currentUser.getUserId(), new BigDecimal("120.00"), PaymentMode.UPI);
        payment.setRazorpayOrderId("order_wallet_502");
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(-502L)).thenReturn(Optional.of(payment));

        RazorpayWalletTopUpVerifyRequestDto request = new RazorpayWalletTopUpVerifyRequestDto();
        request.setPaymentReferenceId(-502L);
        request.setRazorpayOrderId("order_wallet_502");
        request.setRazorpayPaymentId("pay_wallet_502");
        request.setRazorpaySignature("sig_wallet_502");

        assertThrows(PaymentAlreadyProcessedException.class,
                () -> razorpayPaymentService.verifyWalletTopUpPayment(request, currentUser));
    }
}
