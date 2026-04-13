package com.quickbite.cart.cartservice.dto.requestDto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequestDto {
    
    @NotNull
    private Long customerId;

    @NotNull
    private Long restaurantId;

    @NotNull
    private Long menuItemId;

    @NotNull
    private String name;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer quantity;

    private String customization;

}
