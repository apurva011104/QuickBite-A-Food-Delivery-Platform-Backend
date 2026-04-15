package com.quickbite.order.orderservice.mapper;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderItemResponseDto;
import com.quickbite.order.orderservice.entity.OrderItem;

public class OrderItemMapper {
    
    public static OrderItem dtoToEntity(OrderItemRequestDto dto){
        OrderItem item = new OrderItem();
        item.setMenuItemId(dto.getMenuItemId());
        item.setName(dto.getName());
        item.setPrice(dto.getPrice());
        item.setQuantity(dto.getQuantity());
        item.setCustomization(dto.getCustomization());
        return item;
    }

    public static OrderItemResponseDto entityToDto(OrderItem item){
        return new OrderItemResponseDto(item.getOrderItemId(),
                                        item.getMenuItemId(),
                                        item.getName(),
                                        item.getPrice(),
                                        item.getQuantity(), 
                                        item.getCustomization());
    }
}
