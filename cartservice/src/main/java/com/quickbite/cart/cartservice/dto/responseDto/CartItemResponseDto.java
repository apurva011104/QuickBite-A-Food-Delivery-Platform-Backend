package com.quickbite.cart.cartservice.dto.responseDto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

    private Long itemId;

    private Long cartId;

    private Long menuItemId;

    private String name;

    private Integer quantity;

    private BigDecimal price;

    private String customization;
}
