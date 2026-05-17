package com.quickbite.order.orderservice.external.restaurant.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.order.orderservice.dto.responseDto.RestaurantOwnerResponseDto;

@FeignClient(name = "RESTAURANT-SERVICE")
public interface RestaurantClient {

    @GetMapping("/restaurants/internal/{id}/owner")
    RestaurantOwnerResponseDto getOwnerInfo(@PathVariable("id") Long restaurantId);
}
