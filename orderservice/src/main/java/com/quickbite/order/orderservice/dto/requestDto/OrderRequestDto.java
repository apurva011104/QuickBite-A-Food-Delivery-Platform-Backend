package com.quickbite.order.orderservice.dto.requestDto;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.order.orderservice.entity.PaymentMode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String deliveryAddress;

    @NotNull
    @DecimalMin(value = "-90.0", message = "Delivery latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Delivery latitude must be at most 90")
    private BigDecimal deliveryLatitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "Delivery longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Delivery longitude must be at most 180")
    private BigDecimal deliveryLongitude;

    private String specialInstructions;

    @NotNull
    @Size(min = 1)
    private List<OrderItemRequestDto> items;
}
