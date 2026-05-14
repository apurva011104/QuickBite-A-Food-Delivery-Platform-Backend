package com.quickbite.menu.menuservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.quickbite.menu.menuservice.dto.requestDto.CategoryRequestDto;
import com.quickbite.menu.menuservice.dto.requestDto.MenuItemRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.entity.MenuCategory;
import com.quickbite.menu.menuservice.entity.MenuItem;

class MapperTest {

    @Test
    void categoryMapperShouldConvertDtoToEntityAndBack() {
        CategoryRequestDto request = new CategoryRequestDto(1L, "Main Course", "Meals", "img.png", 1);

        MenuCategory entity = CategoryMapper.dtoToEntity(request);
        entity.setCategoryId(10L);

        MenuItem item = new MenuItem(1L, entity, "Paneer Wrap", "Fresh wrap",
                BigDecimal.valueOf(299), BigDecimal.valueOf(249), "wrap.png", true, 320, List.of("veg"));
        item.setItemId(20L);
        entity.setItems(List.of(item));

        CategoryResponseDto response = CategoryMapper.entityToDto(entity);

        assertThat(entity.getName()).isEqualTo("Main Course");
        assertThat(response.getCategoryId()).isEqualTo(10L);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void menuItemMapperShouldConvertDtoToEntityAndBack() {
        MenuCategory category = new MenuCategory(1L, "Main Course", "Meals", "img.png", 1);
        category.setCategoryId(10L);
        MenuItemRequestDto request = new MenuItemRequestDto(1L, 10L, "Paneer Wrap", "Fresh wrap",
                BigDecimal.valueOf(299), BigDecimal.valueOf(249), "wrap.png", true, 320, List.of("veg"));

        MenuItem entity = MenuItemMapper.dtoToEntity(request, category);
        entity.setItemId(20L);

        MenuItemResponseDto response = MenuItemMapper.entityToDto(entity);

        assertThat(entity.getCategory()).isEqualTo(category);
        assertThat(response.getItemId()).isEqualTo(20L);
        assertThat(response.getCategoryId()).isEqualTo(10L);
    }

    @Test
    void categoryMapperShouldReturnEmptyItemsListWhenCategoryItemsNull() {
        MenuCategory category = new MenuCategory(1L, "Desserts", "Sweet", "dessert.png", 2);
        category.setCategoryId(11L);
        category.setItems(null);

        CategoryResponseDto response = CategoryMapper.entityToDto(category);

        assertThat(response.getItems()).isEmpty();
    }
}
