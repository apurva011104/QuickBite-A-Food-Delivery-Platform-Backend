package com.quickbite.order.orderservice.service;

import java.util.List;

import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.security.UserPrincipal;

public interface OrderService {

    OrderResponseDto placeOrder(OrderRequestDto request, UserPrincipal currentUser, String token);

    OrderResponseDto getOrderById(Long orderId, UserPrincipal currentUser, String token);

    List<OrderResponseDto> getOrdersByCustomer(Long customerId, String token);

    List<OrderResponseDto> getOrdersByRestaurant(Long restaurantId, UserPrincipal currentUser, String token);

    List<OrderResponseDto> getActiveOrders();

    OrderResponseDto updateOrderStatus(Long orderId, OrderStatus status, UserPrincipal currentUser, String token);

    OrderResponseDto assignDeliveryAgent(Long orderId, Long agentId);

    OrderResponseDto cancelOrder(Long orderId, UserPrincipal currentUser, String token);

    OrderResponseDto reorderFromHistory(Long orderId, UserPrincipal currentUser, String token);

    Long getOrderCountByRestaurant(Long restaurantId);
}
