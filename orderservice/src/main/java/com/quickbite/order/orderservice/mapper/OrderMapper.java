package com.quickbite.order.orderservice.mapper;

import java.util.List;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderItemResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.Order;

public class OrderMapper {
    
    public static Order dtoToEntity(OrderRequestDto dto){
        Order order = new Order();
        order.setRestaurantId(dto.getRestaurantId());
        order.setDiscount(dto.getDiscount());
        order.setPaymentMode(dto.getPaymentMode());
        order.setDeliveryAddress(dto.getDeliveryAddress());
        order.setDeliveryLatitude(dto.getDeliveryLatitude());
        order.setDeliveryLongitude(dto.getDeliveryLongitude());
        order.setSpecialInstructions(dto.getSpecialInstructions());
        for(OrderItemRequestDto item: dto.getItems()){
            order.addItem( OrderItemMapper.dtoToEntity(item));
        }
        return order;
    }

    public static OrderResponseDto entityToDto(Order order){
        List<OrderItemResponseDto> orderItemDtoList = order.getItems()
                                                        .stream()
                                                        .map(OrderItemMapper::entityToDto)
                                                        .toList();

        return new OrderResponseDto(order.getOrderId(),
                                    order.getCustomerId(),
                                    order.getRestaurantId(),
                                    order.getDeliveryAgentId(),
                                    order.getTotalAmount(),
                                    order.getDiscount(),
                                    order.getFinalAmount(),
                                    order.getPaymentMode(),
                                    order.getOrderStatus(),
                                    order.getOrderDate(),
                                    order.getDeliveryAddress(),
                                    order.getDeliveryLatitude(),
                                    order.getDeliveryLongitude(),
                                    order.getEstimatedDelivery(),
                                    order.getSpecialInstructions(),
                                    orderItemDtoList);
    }

}
