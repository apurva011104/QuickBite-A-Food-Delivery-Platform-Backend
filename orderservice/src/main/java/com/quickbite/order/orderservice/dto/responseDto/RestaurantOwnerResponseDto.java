package com.quickbite.order.orderservice.dto.responseDto;

import lombok.Data;

@Data
public class RestaurantOwnerResponseDto {

    private Long restaurantId;
    private Long ownerId;
    private String name;
}
