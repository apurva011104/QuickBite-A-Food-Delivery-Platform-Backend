package com.quickbite.menu.menuservice.mapper;

import com.quickbite.menu.menuservice.dto.requestDto.MenuItemRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.entity.MenuCategory;
import com.quickbite.menu.menuservice.entity.MenuItem;

public class MenuItemMapper {
    
    public static MenuItem dtoToEntity(MenuItemRequestDto dto, MenuCategory category) {
        return new MenuItem(dto.getRestaurantId(),
                             category, 
                             dto.getName(), 
                             dto.getDescription(), 
                             dto.getPrice(), 
                             dto.getDiscountedPrice(), 
                             dto.getImageUrl(), 
                             dto.isVeg(), 
                             dto.getCalories(), 
                             dto.getTags());
    }

    public static MenuItemResponseDto entityToDto(MenuItem item){
        
        return new MenuItemResponseDto( item.getItemId(), 
                                        item.getRestaurantId(), 
                                        item.getCategory().getCategoryId(), 
                                        item.getName(), 
                                        item.getDescription(), 
                                        item.getPrice(), 
                                        item.getDiscountedPrice(), item.getImageUrl(), 
                                        item.isVeg(), 
                                        item.getCalories(), 
                                        item.isAvailable(),
                                        item.getRating(), 
                                        item.getTags());
    }
}
