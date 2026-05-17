package com.quickbite.restaurant.restaurantservice.external.order.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantOrderResponseDto;

@FeignClient(name = "ORDERSERVICE")
public interface OrderClient {

    @GetMapping("/orders/restaurant/{restaurantId}")
    List<RestaurantOrderResponseDto> getOrdersByRestaurant(@PathVariable("restaurantId") Long restaurantId,
                                                           @RequestHeader("Authorization") String authorization);
}
