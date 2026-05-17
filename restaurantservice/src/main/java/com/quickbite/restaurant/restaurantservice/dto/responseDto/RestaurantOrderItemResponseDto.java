package com.quickbite.restaurant.restaurantservice.dto.responseDto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantOrderItemResponseDto {

    private Long menuItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String customization;
}
