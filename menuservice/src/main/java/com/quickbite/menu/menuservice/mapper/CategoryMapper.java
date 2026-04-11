package com.quickbite.menu.menuservice.mapper;

import java.util.List;

import com.quickbite.menu.menuservice.dto.requestDto.CategoryRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.entity.MenuCategory;

public class CategoryMapper {
    
    public static MenuCategory dtoToEntity(CategoryRequestDto dto){
        return new MenuCategory(dto.getRestaurantId(),
                                dto.getName(), 
                                dto.getDescription(), 
                                dto.getImageUrl(), 
                                dto.getDisplayOrder());
    }

    public static CategoryResponseDto entityToDto(MenuCategory category){

        List<MenuItemResponseDto> menuItemResDtoList = category.getItems()==null 
                                                        ? List.of()
                                                        : category.getItems().stream()
                                                                    .map(MenuItemMapper::entityToDto)
                                                                    .toList();
                                                                    
        return new CategoryResponseDto(category.getCategoryId(), 
                                        category.getRestaurantId(), 
                                        category.getName(), 
                                        category.getDescription(), 
                                        category.getImageUrl(), 
                                        category.getDisplayOrder(),
                                        menuItemResDtoList);
    }
}
