package com.quickbite.restaurant.restaurantservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantResponseDto {
    private Long restaurantId;

    private String name;
    private String description;
    private String cuisine;

    private String city;

    private Double avgRating;

    private boolean isOpen;

    private Integer estimatedDeliveryMin;
}
