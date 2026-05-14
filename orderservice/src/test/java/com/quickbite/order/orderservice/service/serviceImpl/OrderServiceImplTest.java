package com.quickbite.order.orderservice.service.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentStatus;
import com.quickbite.order.orderservice.entity.Order;
import com.quickbite.order.orderservice.entity.OrderItem;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.entity.PaymentMode;
import com.quickbite.order.orderservice.event.NotificationEvent;
import com.quickbite.order.orderservice.exception.EmptyOrderException;
import com.quickbite.order.orderservice.exception.InvalidOrderStateException;
import com.quickbite.order.orderservice.exception.OrderNotFoundException;
import com.quickbite.order.orderservice.exception.UnauthorizedActionException;
import com.quickbite.order.orderservice.external.payment.client.PaymentClient;
import com.quickbite.order.orderservice.repository.OrderRepository;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.NotificationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private OrderServiceImpl orderService;

    private UserPrincipal customer;
    private UserPrincipal otherCustomer;
    private UserPrincipal agent;
    private OrderRequestDto orderRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, paymentClient);
        ReflectionTestUtils.setField(orderService, "notificationEventPublisher", notificationEventPublisher);

        customer = new UserPrincipal(1L, "customer@quickbite.com", "CUSTOMER");
        otherCustomer = new UserPrincipal(9L, "other@quickbite.com", "CUSTOMER");
        agent = new UserPrincipal(5L, "agent@quickbite.com", "AGENT");

        orderRequest = new OrderRequestDto(
                10L,
                BigDecimal.valueOf(20),
                PaymentMode.UPI,
                "221B Baker Street",
                "Less spicy",
                List.of(
                        new OrderItemRequestDto(101L, "Burger", BigDecimal.valueOf(150), 2, "No onion"),
                        new OrderItemRequestDto(102L, "Fries", BigDecimal.valueOf(50), 1, null)
                )
        );

        order = new Order();
        order.setOrderId(100L);
        order.setCustomerId(1L);
        order.setRestaurantId(10L);
        order.setTotalAmount(BigDecimal.valueOf(350));
        order.setDiscount(BigDecimal.valueOf(20));
        order.setFinalAmount(BigDecimal.valueOf(330));
        order.setPaymentMode(PaymentMode.UPI);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));
        order.setDeliveryAddress("221B Baker Street");
        order.setSpecialInstructions("Less spicy");
        order.addItem(new OrderItem(order, 101L, "Burger", BigDecimal.valueOf(150), 2, "No onion"));
        order.addItem(new OrderItem(order, 102L, "Fries", BigDecimal.valueOf(50), 1, null));
    }

    @Test
    void placeOrderShouldThrowWhenItemsMissing() {
        orderRequest.setItems(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest, customer, "token"))
                .isInstanceOf(EmptyOrderException.class)
                .hasMessage("Order must contain at least one item");
    }

    @Test
    void placeOrderShouldThrowWhenDiscountNegative() {
        orderRequest.setDiscount(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> orderService.placeOrder(orderRequest, customer, "token"))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Discount cannot be negative");
    }

    @Test
    void placeOrderShouldConfirmForPaidOnlinePayment() {
        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        paymentResponse.setStatus(PaymentStatus.PAID);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenReturn(paymentResponse);

        OrderResponseDto result = orderService.placeOrder(orderRequest, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("350");
        assertThat(result.getFinalAmount()).isEqualByComparingTo("330");
        verify(notificationEventPublisher, times(2)).publishOrderNotification(any(NotificationEvent.class));
    }

    @Test
    void placeOrderShouldConfirmForCodPendingPayment() {
        orderRequest.setPaymentMode(PaymentMode.COD);
        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        paymentResponse.setStatus(PaymentStatus.PENDING);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer cod-token"))).thenReturn(paymentResponse);

        OrderResponseDto result = orderService.placeOrder(orderRequest, customer, "cod-token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void placeOrderShouldCancelForFailedPayment() {
        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        paymentResponse.setStatus(PaymentStatus.FAILED);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenReturn(paymentResponse);

        OrderResponseDto result = orderService.placeOrder(orderRequest, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void placeOrderShouldCancelWhenPaymentReturnsNull() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenReturn(null);

        OrderResponseDto result = orderService.placeOrder(orderRequest, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void placeOrderShouldCancelWhenPaymentThrows() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenThrow(new RuntimeException("payment down"));

        OrderResponseDto result = orderService.placeOrder(orderRequest, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void placeOrderShouldFloorFinalAmountAtZero() {
        orderRequest.setDiscount(BigDecimal.valueOf(500));
        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        paymentResponse.setStatus(PaymentStatus.PAID);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenReturn(paymentResponse);

        OrderResponseDto result = orderService.placeOrder(orderRequest, customer, "token");

        assertThat(result.getFinalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void getOrderByIdShouldReturnOrderForCustomerOwner() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderResponseDto result = orderService.getOrderById(100L, customer);

        assertThat(result.getOrderId()).isEqualTo(100L);
    }

    @Test
    void getOrderByIdShouldRejectCustomerWhoDoesNotOwnOrder() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(100L, otherCustomer))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to view this order");
    }

    @Test
    void getOrderByIdShouldRejectWrongAgent() {
        order.setDeliveryAgentId(7L);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(100L, agent))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to view this order");
    }

    @Test
    void getOrderByIdShouldThrowWhenMissing() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(404L, customer))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void queryMethodsShouldMapRepositoryResults() {
        when(orderRepository.findByCustomerIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));
        when(orderRepository.findByRestaurantIdOrderByOrderDateDesc(10L)).thenReturn(List.of(order));
        when(orderRepository.findByOrderStatusIn(any())).thenReturn(List.of(order));
        when(orderRepository.countByRestaurantId(10L)).thenReturn(6L);

        assertThat(orderService.getOrdersByCustomer(1L)).hasSize(1);
        assertThat(orderService.getOrdersByRestaurant(10L)).hasSize(1);
        assertThat(orderService.getActiveOrders()).hasSize(1);
        assertThat(orderService.getOrderCountByRestaurant(10L)).isEqualTo(6L);
    }

    @Test
    void updateOrderStatusShouldAllowValidTransition() {
        order.setOrderStatus(OrderStatus.PLACED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto result = orderService.updateOrderStatus(100L, OrderStatus.CONFIRMED);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatusShouldRejectInvalidTransition() {
        order.setOrderStatus(OrderStatus.PLACED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(100L, OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Invalid status transition from PLACED to DELIVERED");
    }

    @Test
    void updateOrderStatusShouldRejectFromCancelled() {
        order.setOrderStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(100L, OrderStatus.CONFIRMED))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("No further status change allowed from CANCELLED");
    }

    @Test
    void assignDeliveryAgentShouldPersistAgentId() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto result = orderService.assignDeliveryAgent(100L, 77L);

        assertThat(result.getDeliveryAgentId()).isEqualTo(77L);
    }

    @Test
    void cancelOrderShouldRejectUnauthorizedCustomer() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, otherCustomer, "token"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to cancel this order");
    }

    @Test
    void cancelOrderShouldRejectWhenPreparationStarted() {
        order.setOrderStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, customer, "token"))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be cancelled after preparation begins");
    }

    @Test
    void cancelOrderShouldSaveAndRefundForOnlinePayment() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto result = orderService.cancelOrder(100L, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentClient).refundPayment(100L, "Bearertoken");
    }

    @Test
    void cancelOrderShouldNotRefundCodOrder() {
        order.setPaymentMode(PaymentMode.COD);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto result = orderService.cancelOrder(100L, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentClient, never()).refundPayment(any(), any());
    }

    @Test
    void cancelOrderShouldSwallowRefundFailure() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentClient.refundPayment(100L, "Bearertoken")).thenThrow(new RuntimeException("refund down"));

        OrderResponseDto result = orderService.cancelOrder(100L, customer, "token");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void reorderFromHistoryShouldRejectUnauthorizedCustomer() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.reorderFromHistory(100L, otherCustomer, "token"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to reorder this order");
    }

    @Test
    void reorderFromHistoryShouldRecreateOrder() {
        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        paymentResponse.setStatus(PaymentStatus.PAID);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(200L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenReturn(paymentResponse);

        OrderResponseDto result = orderService.reorderFromHistory(100L, customer, "token");

        assertThat(result.getOrderId()).isEqualTo(200L);
        assertThat(result.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void placeOrderShouldSendExpectedPaymentRequest() {
        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        paymentResponse.setStatus(PaymentStatus.PAID);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getOrderId() == null) {
                saved.setOrderId(100L);
            }
            return saved;
        });
        when(paymentClient.processPayment(any(), eq("Bearer token"))).thenReturn(paymentResponse);

        orderService.placeOrder(orderRequest, customer, "token");

        ArgumentCaptor<com.quickbite.order.orderservice.dto.requestDto.PaymentRequestDto> captor =
                ArgumentCaptor.forClass(com.quickbite.order.orderservice.dto.requestDto.PaymentRequestDto.class);
        verify(paymentClient).processPayment(captor.capture(), eq("Bearer token"));
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("330");
        assertThat(captor.getValue().getMode()).isEqualTo(PaymentMode.UPI);
    }
}
