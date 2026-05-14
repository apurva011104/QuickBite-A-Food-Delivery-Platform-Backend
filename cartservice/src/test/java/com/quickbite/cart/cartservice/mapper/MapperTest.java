package com.quickbite.cart.cartservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.entity.Cart;
import com.quickbite.cart.cartservice.entity.CartItem;

class MapperTest {

    @Test
    void cartMapperShouldConvertDtoAndEntity() {
        CartRequestDto request = new CartRequestDto(77L);

        Cart cart = CartMapper.dtoToEntity(request);
        cart.setCartId(100L);
        cart.setCustomerId(1L);
        cart.setTotalPrice(BigDecimal.valueOf(498));

        CartResponseDto response = CartMapper.entityToDto(cart, List.of(
                new CartItemResponseDto(200L, 100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion")));

        assertThat(cart.getRestaurantId()).isEqualTo(77L);
        assertThat(response.getCartId()).isEqualTo(100L);
        assertThat(response.getCartItems()).hasSize(1);
    }

    @Test
    void cartItemMapperShouldConvertDtoAndEntity() {
        CartItemRequestDto request = new CartItemRequestDto(20L, 2, "No onion");

        CartItem entity = CartItemMapper.dtoToEntity(request, 100L);
        entity.setItemId(200L);
        entity.setName("Paneer Wrap");
        entity.setPrice(BigDecimal.valueOf(249));
        entity.setCustomization("No onion");

        CartItemResponseDto response = CartItemMapper.entityToDto(entity);

        assertThat(entity.getCartId()).isEqualTo(100L);
        assertThat(response.getItemId()).isEqualTo(200L);
        assertThat(response.getCustomization()).isEqualTo("No onion");
    }
}
