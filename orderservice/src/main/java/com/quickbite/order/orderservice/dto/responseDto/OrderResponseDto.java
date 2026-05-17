package com.quickbite.order.orderservice.dto.responseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.entity.PaymentMode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;

    private Long customerId;

    private Long restaurantId;

    private Long deliveryAgentId;

    private BigDecimal totalAmount;

    private BigDecimal discount;

    private BigDecimal finalAmount;

    private PaymentMode paymentMode;

    private OrderStatus orderStatus;

    private LocalDateTime orderDate;

    private String deliveryAddress;

    private BigDecimal deliveryLatitude;

    private BigDecimal deliveryLongitude;

    private LocalDateTime estimatedDelivery;

    private String specialInstructions;

    private List<OrderItemResponseDto> items;


}
