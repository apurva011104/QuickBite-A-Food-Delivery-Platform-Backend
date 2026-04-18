package com.quickbite.menu.menuservice.service.serviceImpl;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.menu.menuservice.client.RestaurantOwnerClient;
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
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final RestaurantOwnerClient restaurantOwnerClient;

    public MenuServiceImpl(MenuCategoryRepository categoryRepository,
                           MenuItemRepository itemRepository,
                           RestaurantOwnerClient restaurantOwnerClient) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.restaurantOwnerClient = restaurantOwnerClient;
    }

    @Override
    @Transactional
    public CategoryResponseDto addCategory(CategoryRequestDto dto) {
        validateRestaurantOwnership(dto.getRestaurantId());

        MenuCategory category = CategoryMapper.dtoToEntity(dto);
        MenuCategory saved = categoryRepository.save(category);

        log.info("Category created. categoryId={} restaurantId={}", saved.getCategoryId(), saved.getRestaurantId());
        return CategoryMapper.entityToDto(saved);
    }

    @Override
    @Transactional
    public MenuItemResponseDto addMenuItem(MenuItemRequestDto dto) throws ResourceNotFoundException {
        validateRestaurantOwnership(dto.getRestaurantId());

        MenuCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getRestaurantId().equals(dto.getRestaurantId())) {
            throw new IllegalArgumentException("Category does not belong to the given restaurant");
        }

        validatePrices(dto.getPrice(), dto.getDiscountedPrice());

        MenuItem item = MenuItemMapper.dtoToEntity(dto, category);
        MenuItem saved = itemRepository.save(item);

        log.info("Menu item created. itemId={} restaurantId={} categoryId={}",
                saved.getItemId(), saved.getRestaurantId(), category.getCategoryId());

        return MenuItemMapper.entityToDto(saved);
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
    public List<MenuItemResponseDto> getMenuItemsByCategory(Long categoryId) throws ResourceNotFoundException {
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
    @Transactional
    public MenuItemResponseDto updateMenuItem(Long itemId, MenuItemRequestDto dto) throws ResourceNotFoundException {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        validateRestaurantOwnership(item.getRestaurantId());

        MenuCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getRestaurantId().equals(item.getRestaurantId())) {
            throw new IllegalArgumentException("Category does not belong to the item's restaurant");
        }

        validatePrices(dto.getPrice(), dto.getDiscountedPrice());

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setDiscountedPrice(dto.getDiscountedPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setVeg(dto.isVeg());
        item.setCalories(dto.getCalories());
        item.setTags(dto.getTags());
        item.setCategory(category);

        MenuItem updated = itemRepository.save(item);
        log.info("Menu item updated. itemId={}", updated.getItemId());

        return MenuItemMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto dto) throws ResourceNotFoundException {
        MenuCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        validateRestaurantOwnership(category.getRestaurantId());

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        category.setDisplayOrder(dto.getDisplayOrder());

        MenuCategory updated = categoryRepository.save(category);
        log.info("Category updated. categoryId={}", updated.getCategoryId());

        return CategoryMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public MenuItemResponseDto toggleAvailability(Long itemId, boolean isAvailable) throws ResourceNotFoundException {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        validateRestaurantOwnership(item.getRestaurantId());

        item.setAvailable(isAvailable);
        MenuItem updated = itemRepository.save(item);

        log.info("Item availability changed. itemId={} available={}", itemId, isAvailable);
        return MenuItemMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long itemId) throws ResourceNotFoundException {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        validateRestaurantOwnership(item.getRestaurantId());

        itemRepository.delete(item);
        log.info("Menu item deleted. itemId={}", itemId);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) throws ResourceNotFoundException {
        MenuCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        validateRestaurantOwnership(category.getRestaurantId());

        categoryRepository.delete(category);
        log.info("Category deleted. categoryId={}", categoryId);
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

    private void validateRestaurantOwnership(Long restaurantId) {
        boolean owned = restaurantOwnerClient.isRestaurantOwnedByCurrentOwner(restaurantId);
        if (!owned) {
            throw new IllegalArgumentException("You do not have access to this restaurant");
        }
    }

    private void validatePrices(BigDecimal price, BigDecimal discountedPrice) {
        if (price == null || discountedPrice == null) {
            throw new IllegalArgumentException("Price and discounted price are required");
        }

        if (price.signum() < 0 || discountedPrice.signum() < 0) {
            throw new IllegalArgumentException("Price values cannot be negative");
        }

        if (discountedPrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("Discounted price cannot be greater than actual price");
        }
    }
}