package com.quickbite.payment.paymentservice.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.quickbite.payment.paymentservice.repository.PaymentRepository;
import com.quickbite.payment.paymentservice.repository.WalletRepository;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.NotificationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UserPrincipal customerUser;
    private UserPrincipal adminUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "notificationEventPublisher", notificationEventPublisher);
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        customerUser = new UserPrincipal(10L, "customer@quickbite.com", "CUSTOMER");
        adminUser = new UserPrincipal(99L, "admin@quickbite.com", "ADMIN");
    }

    @Test
    void processPaymentKeepsOnlinePaymentsPendingUntilGatewayVerification() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(200L);
        request.setAmount(new BigDecimal("499.99"));
        request.setMode(PaymentMode.CARD);

        PaymentResponseDto response = paymentService.processPayment(request, customerUser);

        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(PaymentMode.CARD, response.getMode());
        assertNotNull(response.getTransactionId());
        assertTrue(response.getTransactionId().startsWith("CARD-PENDING-"));
        assertNull(response.getPaidAt());
        verify(notificationEventPublisher, never()).publishPaymentNotification(any());
    }

    @Test
    void processPaymentKeepsCodPending() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(201L);
        request.setAmount(new BigDecimal("150.00"));
        request.setMode(PaymentMode.COD);

        PaymentResponseDto response = paymentService.processPayment(request, customerUser);

        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertTrue(response.getTransactionId().startsWith("COD-"));
        verify(notificationEventPublisher, never()).publishPaymentNotification(any(NotificationEvent.class));
    }

    @Test
    void processPaymentDelegatesWalletPayments() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(202L);
        request.setAmount(new BigDecimal("80.00"));
        request.setMode(PaymentMode.WALLET);

        PaymentServiceImpl spyService = spy(paymentService);
        ReflectionTestUtils.setField(spyService, "notificationEventPublisher", notificationEventPublisher);

        PaymentResponseDto expected = new PaymentResponseDto();
        expected.setOrderId(202L);
        expected.setStatus(PaymentStatus.PAID);
        doReturn(expected).when(spyService).payFromWallet(customerUser.getUserId(), 202L, new BigDecimal("80.00"));

        PaymentResponseDto response = spyService.processPayment(request, customerUser);

        assertEquals(PaymentStatus.PAID, response.getStatus());
    }

    @Test
    void processPaymentRejectsInvalidAmount() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(203L);
        request.setAmount(BigDecimal.ZERO);
        request.setMode(PaymentMode.UPI);

        assertThrows(InvalidPaymentModeException.class, () -> paymentService.processPayment(request, customerUser));
    }

    @Test
    void processPaymentRejectsExistingPaidOrder() {
        Payment existing = new Payment(204L, customerUser.getUserId(), new BigDecimal("20.00"), PaymentMode.UPI);
        existing.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(204L)).thenReturn(Optional.of(existing));

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(204L);
        request.setAmount(new BigDecimal("20.00"));
        request.setMode(PaymentMode.UPI);

        assertThrows(PaymentAlreadyProcessedException.class, () -> paymentService.processPayment(request, customerUser));
    }

    @Test
    void getPaymentByOrderHidesAnotherCustomersPayment() {
        Payment payment = new Payment(205L, 44L, new BigDecimal("100.00"), PaymentMode.CARD);
        when(paymentRepository.findByOrderId(205L)).thenReturn(Optional.of(payment));

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByOrder(205L, customerUser));
    }

    @Test
    void getPaymentsByCustomerMapsRepositoryResults() {
        Payment first = new Payment(206L, customerUser.getUserId(), new BigDecimal("20.00"), PaymentMode.CARD);
        Payment second = new Payment(207L, customerUser.getUserId(), new BigDecimal("30.00"), PaymentMode.COD);
        when(paymentRepository.findByCustomerId(customerUser.getUserId())).thenReturn(List.of(first, second));

        List<PaymentResponseDto> payments = paymentService.getPaymentsByCustomer(customerUser.getUserId());

        assertEquals(2, payments.size());
        assertEquals(207L, payments.get(1).getOrderId());
    }

    @Test
    void updatePaymentStatusSetsPaidTimestampWhenNeeded() {
        Payment payment = new Payment(208L, customerUser.getUserId(), new BigDecimal("75.00"), PaymentMode.UPI);
        payment.setPaymentId(5L);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentService.updatePaymentStatus(5L, PaymentStatus.PAID);

        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertNotNull(response.getPaidAt());
    }

    @Test
    void updatePaymentStatusSetsRefundTimestamp() {
        Payment payment = new Payment(209L, customerUser.getUserId(), new BigDecimal("75.00"), PaymentMode.UPI);
        payment.setPaymentId(6L);
        payment.setPaidAt(LocalDateTime.now().minusHours(1));
        when(paymentRepository.findById(6L)).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentService.updatePaymentStatus(6L, PaymentStatus.REFUNDED);

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertNotNull(response.getRefundedAt());
    }

    @Test
    void refundPaymentCreditsWalletForWalletPayments() {
        Payment payment = new Payment(210L, customerUser.getUserId(), new BigDecimal("120.00"), PaymentMode.WALLET);
        payment.setPaymentId(7L);
        payment.setStatus(PaymentStatus.PAID);
        Wallet wallet = new Wallet(customerUser.getUserId());
        wallet.setWalletId(9L);
        wallet.setBalance(new BigDecimal("50.00"));
        when(paymentRepository.findByOrderId(210L)).thenReturn(Optional.of(payment));
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.of(wallet));

        PaymentResponseDto response = paymentService.refundPayment(210L, customerUser);

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("170.00"), wallet.getBalance());
        assertEquals(1, wallet.getStatements().size());
        assertEquals(TransactionType.CREDIT, wallet.getStatements().get(0).getType());
        verify(notificationEventPublisher).publishPaymentNotification(any(NotificationEvent.class));
    }

    @Test
    void refundPaymentRejectsUnpaidPayment() {
        Payment payment = new Payment(211L, customerUser.getUserId(), new BigDecimal("60.00"), PaymentMode.CARD);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(211L)).thenReturn(Optional.of(payment));

        assertThrows(PaymentAlreadyProcessedException.class, () -> paymentService.refundPayment(211L, customerUser));
    }

    @Test
    void refundPaymentAllowsAdminToRefundAnyCustomer() {
        Payment payment = new Payment(212L, 77L, new BigDecimal("90.00"), PaymentMode.CARD);
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(212L)).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentService.refundPayment(212L, adminUser);

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
    }

    @Test
    void getWalletCreatesWalletWhenMissing() {
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.empty());

        WalletResponseDto response = paymentService.getWallet(customerUser.getUserId());

        assertEquals(customerUser.getUserId(), response.getCustomerId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void addToWalletUpdatesBalanceAndCreatesStatement() {
        Wallet wallet = new Wallet(customerUser.getUserId());
        wallet.setWalletId(11L);
        wallet.setBalance(new BigDecimal("25.00"));
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.of(wallet));

        WalletResponseDto response = paymentService.addToWallet(customerUser.getUserId(), new BigDecimal("75.00"));

        assertEquals(new BigDecimal("100.00"), response.getBalance());
        assertEquals(1, response.getStatements().size());
        assertEquals(TransactionType.CREDIT, response.getStatements().get(0).getType());
        verify(notificationEventPublisher).publishPaymentNotification(any(NotificationEvent.class));
    }

    @Test
    void addToWalletRejectsInvalidAmount() {
        assertThrows(InvalidPaymentModeException.class,
                () -> paymentService.addToWallet(customerUser.getUserId(), BigDecimal.ZERO));
    }

    @Test
    void payFromWalletRejectsInsufficientBalance() {
        Wallet wallet = new Wallet(customerUser.getUserId());
        wallet.setBalance(new BigDecimal("10.00"));
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class,
                () -> paymentService.payFromWallet(customerUser.getUserId(), 301L, new BigDecimal("50.00")));
    }

    @Test
    void payFromWalletRejectsMissingWallet() {
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> paymentService.payFromWallet(customerUser.getUserId(), 302L, new BigDecimal("50.00")));
    }

    @Test
    void payFromWalletCreatesPaidPaymentAndDebitsWallet() {
        Wallet wallet = new Wallet(customerUser.getUserId());
        wallet.setBalance(new BigDecimal("150.00"));
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.of(wallet));

        PaymentResponseDto response = paymentService.payFromWallet(customerUser.getUserId(), 303L, new BigDecimal("40.00"));

        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertEquals(new BigDecimal("110.00"), wallet.getBalance());
        assertEquals(1, wallet.getStatements().size());
        assertTrue(response.getTransactionId().startsWith("WALLET-"));
    }

    @Test
    void getWalletStatementsMapsExistingStatements() {
        Wallet wallet = new Wallet(customerUser.getUserId());
        WalletStatement statement = new WalletStatement(new BigDecimal("15.00"), TransactionType.CREDIT, "Wallet top-up");
        statement.setStatementId(41L);
        wallet.addStatement(statement);
        when(walletRepository.findByCustomerId(customerUser.getUserId())).thenReturn(Optional.of(wallet));

        List<WalletStatementResponseDto> response = paymentService.getWalletStatements(customerUser.getUserId());

        assertEquals(1, response.size());
        assertEquals(41L, response.get(0).getStatementId());
    }

    @Test
    void refundPaymentRejectsCustomerTryingAnotherOrder() {
        Payment payment = new Payment(213L, 88L, new BigDecimal("15.00"), PaymentMode.CARD);
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(213L)).thenReturn(Optional.of(payment));

        assertThrows(PaymentNotFoundException.class, () -> paymentService.refundPayment(213L, customerUser));
    }

    @Test
    void payFromWalletRejectsExistingPaidOrder() {
        Payment existing = new Payment(304L, customerUser.getUserId(), new BigDecimal("20.00"), PaymentMode.WALLET);
        existing.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findByOrderId(304L)).thenReturn(Optional.of(existing));

        assertThrows(PaymentAlreadyProcessedException.class,
                () -> paymentService.payFromWallet(customerUser.getUserId(), 304L, new BigDecimal("20.00")));
        verify(walletRepository, never()).findByCustomerId(customerUser.getUserId());
    }
}
