package com.quickbite.cart.cartservice.mapper;

import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.entity.CartItem;

public class CartItemMapper {
    
    public static CartItem dtoToEntity(CartItemRequestDto dto, Long cartId){
        CartItem item = new CartItem();
        item.setCartId(cartId);
        item.setMenuItemId(dto.getMenuItemId());
        item.setQuantity(dto.getQuantity());
        return item;
    }

    public static CartItemResponseDto entityToDto(CartItem item){
        return new CartItemResponseDto(item.getItemId(),
                                        item.getCartId(),
                                        item.getMenuItemId(),
                                        item.getName(),
                                        item.getQuantity(),
                                        item.getPrice(),
                                        item.getCustomization());
    }
}
