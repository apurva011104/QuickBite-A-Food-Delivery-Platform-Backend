package com.quickbite.restaurant.restaurantservice.service;

import java.util.List;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.OwnerRestaurantDetailsResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantOwnerResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;

public interface RestaurantService {
    
    //Owner methods
    RestaurantResponseDto registerRestaurant(RestaurantRequestDto request);
    List<RestaurantResponseDto> getByOwner();
    OwnerRestaurantDetailsResponseDto getOwnerRestaurantDetails(Long id, String authorizationHeader);
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
    List<RestaurantResponseDto> getPendingRestaurants();
    RestaurantResponseDto rejectRestaurant(Long id, String reason);

    //System methods 
    RestaurantResponseDto updateRating(Long id, Double rating);
    RestaurantOwnerResponseDto getRestaurantOwnerInfo(Long id);

}
