package com.quickbite.cart.cartservice.mapper;

import java.util.List;

import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.entity.Cart;

public class CartMapper {
    
    public static Cart dtoToEntity(CartRequestDto dto){
        Cart cart = new Cart();
        cart.setRestaurantId(dto.getRestaurantId());
        return cart;
    }

    public static CartResponseDto entityToDto(Cart cart, List<CartItemResponseDto> cartItems){
        return new CartResponseDto(cart.getCartId(), 
                                    cart.getCustomerId(), 
                                    cart.getRestaurantId(), 
                                    cart.getTotalPrice(),
                                    cartItems);
    }
}
