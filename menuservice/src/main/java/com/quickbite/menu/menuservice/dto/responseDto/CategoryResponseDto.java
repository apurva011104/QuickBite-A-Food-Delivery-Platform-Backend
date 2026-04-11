package com.quickbite.menu.menuservice.dto.responseDto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    
    private Long categoryId;

    private Long restaurantId;

    private String name;

    private String description;

    private String imageUrl;

    private int displayOrder;

    private List<MenuItemResponseDto> items;
}
