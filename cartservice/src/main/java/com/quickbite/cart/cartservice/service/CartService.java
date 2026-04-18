package com.quickbite.cart.cartservice.service;

import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;

public interface CartService {

    CartResponseDto getCartByCustomerId(Long customerId);

    CartResponseDto addItemToCart(Long customerId, CartItemRequestDto dto);

    void removeItemFromCart(Long customerId, Long itemId);

    CartItemResponseDto updateCartItemQuantity(Long customerId, Long itemId, Integer quantity);

    void clearCart(Long customerId);

    CartResponseDto changeRestaurant(Long customerId, CartRequestDto dto);

    CartResponseDto applyPromoCode(Long customerId, String promoCode);
}