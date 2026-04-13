package com.quickbite.cart.cartservice.dto.responseDto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    
    private Long cartId;

    private Long customerId;

    private Long restaurantId;

    private BigDecimal totalPrice;

    private List<CartItemResponseDto> cartItems;
}
