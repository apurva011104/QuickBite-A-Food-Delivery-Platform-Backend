package com.quickbite.restaurant.restaurantservice.service;

import java.util.List;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;

public interface RestaurantService {
    
    //Owner methods
    RestaurantResponseDto registerRestaurant(RestaurantRequestDto request);
    List<RestaurantResponseDto> getByOwner();
    RestaurantResponseDto updateRestaurant(Long id, RestaurantRequestDto request);
    RestaurantResponseDto toggleOpen(Long id);
    void deleteRestaurant(Long id);

    //Public methods
    RestaurantResponseDto getById(Long id);
    List<RestaurantResponseDto> getByCity(String city);
    List<RestaurantResponseDto> getByCuisine(String cuisine);
    List<RestaurantResponseDto> searchRestaurants(String keyword);
    List<RestaurantResponseDto> getNearby(Double lat, Double lng, Double radius);

    //Admin methods
    RestaurantResponseDto approveRestaurant(Long id);

    //System methods 
    RestaurantResponseDto updateRating(Long id, Double rating);

}
