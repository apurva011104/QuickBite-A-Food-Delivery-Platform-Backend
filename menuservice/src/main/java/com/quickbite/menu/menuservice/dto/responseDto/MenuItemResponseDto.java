package com.quickbite.menu.menuservice.dto.responseDto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemResponseDto {
    
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
