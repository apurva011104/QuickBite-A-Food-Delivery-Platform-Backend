package com.quickbite.cart.cartservice.dto.external;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class MenuItemSnapshotDto {
    private Long itemId;
    private Long restaurantId;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String imageUrl;
    private boolean isVeg;
    private double calories;
    private boolean isAvailable;
    private double rating;
    private List<String> tags;
}