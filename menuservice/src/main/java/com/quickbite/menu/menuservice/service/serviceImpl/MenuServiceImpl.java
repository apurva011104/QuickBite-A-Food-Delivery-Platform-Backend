package com.quickbite.menu.menuservice.service.serviceImpl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quickbite.menu.menuservice.dto.requestDto.CategoryRequestDto;
import com.quickbite.menu.menuservice.dto.requestDto.MenuItemRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.entity.MenuCategory;
import com.quickbite.menu.menuservice.entity.MenuItem;
import com.quickbite.menu.menuservice.exception.ResourceNotFoundException;
import com.quickbite.menu.menuservice.mapper.CategoryMapper;
import com.quickbite.menu.menuservice.mapper.MenuItemMapper;
import com.quickbite.menu.menuservice.repository.MenuCategoryRepository;
import com.quickbite.menu.menuservice.repository.MenuItemRepository;
import com.quickbite.menu.menuservice.service.MenuService;

@Service
public class MenuServiceImpl implements MenuService{
    
    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Override
    public CategoryResponseDto addCategory(CategoryRequestDto dto) {
        MenuCategory category = CategoryMapper.dtoToEntity(dto);
        return CategoryMapper.entityToDto(categoryRepository.save(category));
    }

    @Override
    public MenuItemResponseDto addMenuItem(MenuItemRequestDto dto) throws ResourceNotFoundException {
        MenuCategory category = categoryRepository.findById(dto.getCategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        MenuItem item = MenuItemMapper.dtoToEntity(dto, category);
        return MenuItemMapper.entityToDto(itemRepository.save(item));
    }

    @Override
    public List<MenuItemResponseDto> getItemsByRestaurant(Long restaurantId) {
        return itemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(MenuItemMapper::entityToDto)
                .toList();
    }

    @Override
    public List<CategoryResponseDto> getCategoriesByRestaurant(Long restaurantId) {
        return categoryRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(CategoryMapper::entityToDto)
                .toList();
    }

    @Override
    public List<CategoryResponseDto> getFullMenuByRestaurant(Long restaurantId) {
        return categoryRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(CategoryMapper::entityToDto)
                .toList();
    }

    @Override
    public List<MenuItemResponseDto> getMenuItemsByCategory(Long categoryId) throws ResourceNotFoundException{

        MenuCategory category = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return itemRepository.findByCategory(category)
                .stream()
                .map(MenuItemMapper::entityToDto)
                .toList();
    }

    @Override
    public MenuItemResponseDto getMenuItemById(Long itemId) throws ResourceNotFoundException {
        MenuItem item = itemRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        return MenuItemMapper.entityToDto(item);
    }

    @Override
    public CategoryResponseDto getCategoryById(Long categoryId) throws ResourceNotFoundException {
        MenuCategory category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return CategoryMapper.entityToDto(category);
    }

    @Override
    public MenuItemResponseDto updateMenuItem(Long itemId, MenuItemRequestDto dto) throws ResourceNotFoundException{
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        MenuCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setDiscountedPrice(dto.getDiscountedPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setVeg(dto.isVeg());
        item.setCalories(dto.getCalories());
        item.setTags(dto.getTags());
        item.setCategory(category);
        return MenuItemMapper.entityToDto(itemRepository.save(item));
    }

    @Override
    public CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto dto) throws ResourceNotFoundException{
        MenuCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        category.setDisplayOrder(dto.getDisplayOrder());
        return CategoryMapper.entityToDto(categoryRepository.save(category));
    }

    @Override
    public MenuItemResponseDto toggleAvailability(Long itemId, boolean isAvailable) throws ResourceNotFoundException {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        item.setAvailable(isAvailable);
        return MenuItemMapper.entityToDto(itemRepository.save(item));
    }

    @Override
    public void deleteMenuItem(Long itemId) throws ResourceNotFoundException{
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found");
        }
        itemRepository.deleteById(itemId);
    }

    @Override
    public void deleteCategory(Long categoryId) throws ResourceNotFoundException{
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(categoryId);
    }

    @Override
    public List<MenuItemResponseDto> searchMenuItems(String query) {
        return itemRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(MenuItemMapper::entityToDto)
                .toList();
    }

    @Override
    public List<MenuItemResponseDto> getItemsByVeg(boolean isVeg) {
        return itemRepository.findByIsVeg(isVeg)
                .stream()
                .map(MenuItemMapper::entityToDto)
                .toList();
    }

    @Override
    public List<MenuItemResponseDto> getItemsBelowPrice(BigDecimal price) {
        return itemRepository.findByPriceLessThanEqual(price)
                .stream()
                .map(MenuItemMapper::entityToDto)
                .toList();
    }
    
}
