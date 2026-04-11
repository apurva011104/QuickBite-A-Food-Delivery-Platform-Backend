package com.quickbite.menu.menuservice.service;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.menu.menuservice.dto.requestDto.CategoryRequestDto;
import com.quickbite.menu.menuservice.dto.requestDto.MenuItemRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.exception.ResourceNotFoundException;
import com.quickbite.menu.menuservice.exception.RestaurantNotFoundException;

public interface MenuService {

    CategoryResponseDto addCategory(CategoryRequestDto menuCategoryDto);

    MenuItemResponseDto addMenuItem(MenuItemRequestDto menuItemDto) throws ResourceNotFoundException;

    List<MenuItemResponseDto> getItemsByRestaurant(Long restaurantId) throws RestaurantNotFoundException;

    List<CategoryResponseDto> getCategoriesByRestaurant(Long restaurantId) throws RestaurantNotFoundException;

    MenuItemResponseDto getMenuItemById(Long itemId) throws ResourceNotFoundException;

    CategoryResponseDto getCategoryById(Long categoryId) throws ResourceNotFoundException;

    MenuItemResponseDto updateMenuItem(Long itemId, MenuItemRequestDto menuItemDto) throws ResourceNotFoundException;

    CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto menuCategoryDto) throws ResourceNotFoundException;

    MenuItemResponseDto toggleAvailability(Long itemId, boolean isAvailable) throws ResourceNotFoundException;

    void deleteMenuItem(Long itemId) throws ResourceNotFoundException;

    void deleteCategory(Long categoryId) throws ResourceNotFoundException;

    List<MenuItemResponseDto> searchMenuItems(String query);

    List<MenuItemResponseDto> getItemsByVeg(boolean isVeg);

    List<MenuItemResponseDto> getMenuItemsByCategory(Long categoryId) throws ResourceNotFoundException;

    List<CategoryResponseDto> getFullMenuByRestaurant(Long restaurantId) throws RestaurantNotFoundException;

    List<MenuItemResponseDto> getItemsBelowPrice(BigDecimal price);
}
