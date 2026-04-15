package com.quickbite.order.orderservice.service;

import java.util.List;

import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.OrderStatus;

public interface OrderService {

    OrderResponseDto placeOrder(OrderRequestDto request, String token);

    OrderResponseDto getOrderById(Long orderId);

    List<OrderResponseDto> getOrdersByCustomer(String token);

    List<OrderResponseDto> getOrdersByRestaurant(Long restaurantId);

    List<OrderResponseDto> getActiveOrders();

    OrderResponseDto updateOrderStatus(Long orderId, OrderStatus status);

    OrderResponseDto assignDeliveryAgent(Long orderId, Long agentId);

    OrderResponseDto cancelOrder(Long orderId, String token);

    OrderResponseDto reorderFromHistory(Long orderId, String token);

    Long getOrderCountByRestaurant(Long restaurantId);
}