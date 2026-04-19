package com.quickbite.review.reviewservice.external.restaurant.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "RESTAURANTSERVICE")
public interface RestaurantClient {

    @PutMapping("/restaurants/internal/rating/{id}")
    void updateRestaurantRating(@PathVariable("id") Long restaurantId,
                                @RequestParam("rating") Double rating);
}