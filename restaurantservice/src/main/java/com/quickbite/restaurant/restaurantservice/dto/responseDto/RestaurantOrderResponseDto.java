package com.quickbite.restaurant.restaurantservice.dto.responseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantOrderResponseDto {

    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private Long deliveryAgentId;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private String paymentMode;
    private String orderStatus;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;
    private String deliveryAddress;
    private String specialInstructions;
    private List<RestaurantOrderItemResponseDto> items;
}
