package com.quickbite.order.orderservice.dto.requestDto;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.order.orderservice.entity.PaymentMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {
    
    @NotNull
    private Long restaurantId;

    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull
    private PaymentMode paymentMode;

    @NotNull
    private String deliveryAddress;

    private String specialInstructions;

    @NotNull
    @Size(min = 1)
    private List<OrderItemRequestDto> items;
}
