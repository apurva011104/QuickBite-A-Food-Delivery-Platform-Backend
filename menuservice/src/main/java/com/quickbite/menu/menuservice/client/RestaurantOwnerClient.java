package com.quickbite.menu.menuservice.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.quickbite.menu.menuservice.dto.external.RestaurantSummaryDto;

@FeignClient(name = "RESTAURANTSERVICE")
public interface RestaurantOwnerClient {

    @GetMapping("/restaurants/owner/my")
    List<RestaurantSummaryDto> getMyRestaurants();
}