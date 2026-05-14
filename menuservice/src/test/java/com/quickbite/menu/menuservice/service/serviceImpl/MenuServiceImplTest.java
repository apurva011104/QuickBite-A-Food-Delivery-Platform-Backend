package com.quickbite.menu.menuservice.service.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.menu.menuservice.client.RestaurantOwnerClient;
import com.quickbite.menu.menuservice.dto.external.RestaurantSummaryDto;
import com.quickbite.menu.menuservice.dto.requestDto.CategoryRequestDto;
import com.quickbite.menu.menuservice.dto.requestDto.MenuItemRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.entity.MenuCategory;
import com.quickbite.menu.menuservice.entity.MenuItem;
import com.quickbite.menu.menuservice.exception.ResourceNotFoundException;
import com.quickbite.menu.menuservice.repository.MenuCategoryRepository;
import com.quickbite.menu.menuservice.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuCategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository itemRepository;

    @Mock
    private RestaurantOwnerClient restaurantOwnerClient;

    @InjectMocks
    private MenuServiceImpl menuService;

    private CategoryRequestDto categoryRequestDto;
    private MenuItemRequestDto menuItemRequestDto;
    private MenuCategory category;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        categoryRequestDto = new CategoryRequestDto(1L, "Main Course", "Meals", "img.png", 1);
        menuItemRequestDto = new MenuItemRequestDto(
                1L,
                10L,
                "Paneer Wrap",
                "Fresh wrap",
                BigDecimal.valueOf(299),
                BigDecimal.valueOf(249),
                "wrap.png",
                true,
                320,
                List.of("veg", "popular"));

        category = new MenuCategory(1L, "Main Course", "Meals", "img.png", 1);
        category.setCategoryId(10L);

        item = new MenuItem(1L, category, "Paneer Wrap", "Fresh wrap",
                BigDecimal.valueOf(299), BigDecimal.valueOf(249), "wrap.png", true, 320, List.of("veg"));
        item.setItemId(20L);
        item.setAvailable(true);
        item.setRating(4.5);
    }

    @Test
    void addCategoryShouldSaveAndReturnDtoWhenRestaurantOwned() {
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(categoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> {
            MenuCategory saved = invocation.getArgument(0);
            saved.setCategoryId(10L);
            return saved;
        });

        CategoryResponseDto result = menuService.addCategory(categoryRequestDto);

        assertThat(result.getCategoryId()).isEqualTo(10L);
        assertThat(result.getRestaurantId()).isEqualTo(1L);
        verify(categoryRepository).save(any(MenuCategory.class));
    }

    @Test
    void addCategoryShouldRejectWhenRestaurantNotOwned() {
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(2L, null, null, null, null, null, true, true, null)));

        assertThatThrownBy(() -> menuService.addCategory(categoryRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You do not have access to this restaurant");

        verify(categoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void addMenuItemShouldSaveWhenCategoryMatchesRestaurant() throws Exception {
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem saved = invocation.getArgument(0);
            saved.setItemId(20L);
            return saved;
        });

        MenuItemResponseDto result = menuService.addMenuItem(menuItemRequestDto);

        assertThat(result.getItemId()).isEqualTo(20L);
        assertThat(result.getRestaurantId()).isEqualTo(1L);
        verify(itemRepository).save(any(MenuItem.class));
    }

    @Test
    void addMenuItemShouldThrowWhenCategoryMissing() {
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.addMenuItem(menuItemRequestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void addMenuItemShouldThrowWhenCategoryBelongsToAnotherRestaurant() {
        MenuCategory foreignCategory = new MenuCategory(99L, "Other", "Other", "other.png", 1);
        foreignCategory.setCategoryId(10L);
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(foreignCategory));

        assertThatThrownBy(() -> menuService.addMenuItem(menuItemRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category does not belong to the given restaurant");
    }

    @Test
    void addMenuItemShouldThrowWhenDiscountExceedsPrice() {
        MenuItemRequestDto invalidDto = new MenuItemRequestDto(1L, 10L, "Paneer Wrap", "Fresh wrap",
                BigDecimal.valueOf(200), BigDecimal.valueOf(250), "wrap.png", true, 320, List.of("veg"));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> menuService.addMenuItem(invalidDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discounted price cannot be greater than actual price");
    }

    @Test
    void getItemsByRestaurantShouldMapRepositoryResults() {
        when(itemRepository.findByRestaurantId(1L)).thenReturn(List.of(item));

        List<MenuItemResponseDto> result = menuService.getItemsByRestaurant(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Paneer Wrap");
    }

    @Test
    void getCategoriesByRestaurantShouldMapRepositoryResults() {
        category.setItems(List.of(item));
        when(categoryRepository.findByRestaurantId(1L)).thenReturn(List.of(category));

        List<CategoryResponseDto> result = menuService.getCategoriesByRestaurant(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItems()).hasSize(1);
    }

    @Test
    void getFullMenuByRestaurantShouldReturnMappedCategories() {
        when(categoryRepository.findByRestaurantId(1L)).thenReturn(List.of(category));

        List<CategoryResponseDto> result = menuService.getFullMenuByRestaurant(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Main Course");
    }

    @Test
    void getMenuItemsByCategoryShouldThrowWhenCategoryMissing() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.getMenuItemsByCategory(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void getMenuItemByIdShouldMapEntity() throws Exception {
        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));

        MenuItemResponseDto result = menuService.getMenuItemById(20L);

        assertThat(result.getItemId()).isEqualTo(20L);
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void getCategoryByIdShouldMapEntity() throws Exception {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        CategoryResponseDto result = menuService.getCategoryById(10L);

        assertThat(result.getCategoryId()).isEqualTo(10L);
    }

    @Test
    void updateMenuItemShouldPersistModifiedFields() throws Exception {
        MenuItemRequestDto updateDto = new MenuItemRequestDto(1L, 10L, "Updated Wrap", "Updated description",
                BigDecimal.valueOf(350), BigDecimal.valueOf(300), "updated.png", false, 410, List.of("spicy"));

        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponseDto result = menuService.updateMenuItem(20L, updateDto);

        assertThat(result.getName()).isEqualTo("Updated Wrap");
        assertThat(result.getPrice()).isEqualByComparingTo("350");
        assertThat(result.isVeg()).isFalse();
    }

    @Test
    void updateMenuItemShouldRejectCategoryFromAnotherRestaurant() {
        MenuCategory foreignCategory = new MenuCategory(77L, "Foreign", "Foreign", "foreign.png", 1);
        foreignCategory.setCategoryId(10L);

        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(foreignCategory));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));

        assertThatThrownBy(() -> menuService.updateMenuItem(20L, menuItemRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category does not belong to the item's restaurant");
    }

    @Test
    void updateCategoryShouldPersistModifiedFields() throws Exception {
        CategoryRequestDto updateDto = new CategoryRequestDto(1L, "Sides", "Quick bites", "sides.png", 3);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(categoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponseDto result = menuService.updateCategory(10L, updateDto);

        assertThat(result.getName()).isEqualTo("Sides");
        assertThat(result.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void toggleAvailabilityShouldUpdateFlag() throws Exception {
        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuItemResponseDto result = menuService.toggleAvailability(20L, false);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void deleteMenuItemShouldDeleteOwnedItem() throws Exception {
        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));

        menuService.deleteMenuItem(20L);

        verify(itemRepository).delete(item);
    }

    @Test
    void deleteCategoryShouldDeleteOwnedCategory() throws Exception {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(restaurantOwnerClient.getMyRestaurants()).thenReturn(List.of(new RestaurantSummaryDto(1L, null, null, null, null, null, true, true, null)));

        menuService.deleteCategory(10L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void searchAndFilterMethodsShouldDelegateToRepository() {
        when(itemRepository.findByNameContainingIgnoreCase("wrap")).thenReturn(List.of(item));
        when(itemRepository.findByIsVeg(true)).thenReturn(List.of(item));
        when(itemRepository.findByPriceLessThanEqual(BigDecimal.valueOf(300))).thenReturn(List.of(item));

        assertThat(menuService.searchMenuItems("wrap")).hasSize(1);
        assertThat(menuService.getItemsByVeg(true)).hasSize(1);
        assertThat(menuService.getItemsBelowPrice(BigDecimal.valueOf(300))).hasSize(1);
    }
}
