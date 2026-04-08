package com.quickbite.restaurant.restaurantservice.mapper;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;
import com.quickbite.restaurant.restaurantservice.entity.Restaurant;

public class RestaurantMapper {

    public static RestaurantResponseDto mapToDto(Restaurant restaurant) {

        RestaurantResponseDto dto = new RestaurantResponseDto();

        dto.setRestaurantId(restaurant.getRestaurantId());
        dto.setName(restaurant.getName());
        dto.setDescription(restaurant.getDescription());
        dto.setCuisine(restaurant.getCuisine());
        dto.setCity(restaurant.getCity());
        dto.setAvgRating(restaurant.getAvgRating());
        dto.setOpen(restaurant.isOpen());
        dto.setEstimatedDeliveryMin(restaurant.getEstimatedDeliveryMin());

        return dto;
    }

    public static Restaurant mapToEntity(RestaurantRequestDto dto){
        Restaurant restaurant = new Restaurant();

        restaurant.setName(dto.getName());
        restaurant.setDescription(dto.getDescription());
        restaurant.setCuisine(dto.getCuisine());
        restaurant.setAddress(dto.getAddress());
        restaurant.setCity(dto.getCity());
        restaurant.setLatitude(dto.getLatitude());
        restaurant.setLongitude(dto.getLongitude());
        restaurant.setPhone(dto.getPhone());
        restaurant.setDeliveryRadius(dto.getDeliveryRadius());
        restaurant.setMinOrderAmount(dto.getMinOrderAmount());
        restaurant.setEstimatedDeliveryMin(dto.getEstimatedDeliveryMin());

        return restaurant;
    }
}
