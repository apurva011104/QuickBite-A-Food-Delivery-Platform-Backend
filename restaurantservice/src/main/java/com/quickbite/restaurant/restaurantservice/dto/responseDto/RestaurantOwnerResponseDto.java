package com.quickbite.restaurant.restaurantservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantOwnerResponseDto {

    private Long restaurantId;
    private Long ownerId;
    private String name;
}
