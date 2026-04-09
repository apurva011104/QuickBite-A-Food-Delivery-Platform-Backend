package com.quickbite.restaurant.restaurantservice.dto.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRequestDto {

    private String name;
    private String description;
    private String cuisine;

    private String address;
    private String city;

    private Double latitude;
    private Double longitude;

    private String phone;

    private Double deliveryRadius;
    private Double minOrderAmount;

    private Integer estimatedDeliveryMin;
}
