package com.quickbite.restaurant.restaurantservice.dto.responseDto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRestaurantDetailsResponseDto {

    private RestaurantResponseDto restaurant;
    private List<RestaurantOrderResponseDto> orders;
}
