package com.quickbite.order.orderservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderItemResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.entity.PaymentMode;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;
    private UsernamePasswordAuthenticationToken authentication;
    private UserPrincipal customer;

    @BeforeEach
    void setUp() {
        orderController = new OrderController();
        org.springframework.test.util.ReflectionTestUtils.setField(orderController, "orderService", orderService);
        customer = new UserPrincipal(1L, "customer@quickbite.com", "CUSTOMER");
        authentication = new UsernamePasswordAuthenticationToken(customer, null, List.of(() -> "ROLE_CUSTOMER"));
    }

    @Test
    void placeOrderShouldDelegateWithExtractedToken() {
        OrderRequestDto request = orderRequest();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer token123");
        when(orderService.placeOrder(request, customer, "token123")).thenReturn(orderResponse());

        ResponseEntity<OrderResponseDto> response = orderController.placeOrder(request, authentication, servletRequest);

        assertThat(response.getBody().getOrderId()).isEqualTo(100L);
    }

    @Test
    void getOrderByIdShouldDelegateToService() {
        when(orderService.getOrderById(100L, customer)).thenReturn(orderResponse());

        ResponseEntity<OrderResponseDto> response = orderController.getOrderById(100L, authentication);

        assertThat(response.getBody().getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void getOrdersByCustomerShouldUseAuthenticatedUser() {
        when(orderService.getOrdersByCustomer(1L)).thenReturn(List.of(orderResponse()));

        ResponseEntity<List<OrderResponseDto>> response = orderController.getOrdersByCustomer(authentication);

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void updateOrderStatusShouldDelegate() {
        when(orderService.updateOrderStatus(100L, OrderStatus.CONFIRMED)).thenReturn(orderResponse());

        ResponseEntity<OrderResponseDto> response = orderController.updateOrderStatus(100L, OrderStatus.CONFIRMED);

        assertThat(response.getBody().getOrderId()).isEqualTo(100L);
    }

    @Test
    void assignDeliveryAgentShouldDelegate() {
        when(orderService.assignDeliveryAgent(100L, 88L)).thenReturn(orderResponse());

        ResponseEntity<OrderResponseDto> response = orderController.assignDeliveryAgent(100L, 88L);

        assertThat(response.getBody().getOrderId()).isEqualTo(100L);
    }

    @Test
    void cancelOrderShouldExtractTokenAndDelegate() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer token123");
        when(orderService.cancelOrder(100L, customer, "token123")).thenReturn(orderResponse());

        ResponseEntity<OrderResponseDto> response = orderController.cancelOrder(100L, authentication, servletRequest);

        assertThat(response.getBody().getOrderId()).isEqualTo(100L);
    }

    @Test
    void reorderShouldExtractTokenAndDelegate() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer token123");
        when(orderService.reorderFromHistory(100L, customer, "token123")).thenReturn(orderResponse());

        ResponseEntity<OrderResponseDto> response = orderController.reorder(100L, authentication, servletRequest);

        assertThat(response.getBody().getOrderId()).isEqualTo(100L);
    }

    @Test
    void getActiveOrdersAndCountsShouldDelegate() {
        when(orderService.getActiveOrders()).thenReturn(List.of(orderResponse()));
        when(orderService.getOrderCountByRestaurant(10L)).thenReturn(5L);
        when(orderService.getOrdersByRestaurant(10L)).thenReturn(List.of(orderResponse()));

        assertThat(orderController.getActiveOrders().getBody()).hasSize(1);
        assertThat(orderController.getOrderCount(10L).getBody()).isEqualTo(5L);
        assertThat(orderController.getOrdersByRestaurant(10L).getBody()).hasSize(1);
        verify(orderService).getOrdersByRestaurant(10L);
    }

    @Test
    void placeOrderShouldThrowWhenAuthorizationHeaderMissing() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> orderController.placeOrder(orderRequest(), authentication, new MockHttpServletRequest())).getMessage())
                .isEqualTo("Missing or invalid Authorization header");
    }

    private OrderRequestDto orderRequest() {
        return new OrderRequestDto(10L, BigDecimal.ZERO, PaymentMode.UPI, "221B Baker Street", "Less spicy",
                List.of(new OrderItemRequestDto(101L, "Burger", BigDecimal.valueOf(150), 2, "No onion")));
    }

    private OrderResponseDto orderResponse() {
        return new OrderResponseDto(100L, 1L, 10L, null, BigDecimal.valueOf(300), BigDecimal.ZERO,
                BigDecimal.valueOf(300), PaymentMode.UPI, OrderStatus.CONFIRMED, null,
                "221B Baker Street", null, "Less spicy",
                List.of(new OrderItemResponseDto(1L, 101L, "Burger", BigDecimal.valueOf(150), 2, "No onion")));
    }
}
