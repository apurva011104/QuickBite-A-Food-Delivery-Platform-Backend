package com.quickbite.menu.menuservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.menu.menuservice.dto.requestDto.CategoryRequestDto;
import com.quickbite.menu.menuservice.dto.requestDto.MenuItemRequestDto;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.exception.ResourceNotFoundException;
import com.quickbite.menu.menuservice.exception.RestaurantNotFoundException;
import com.quickbite.menu.menuservice.service.MenuService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    // ================= CATEGORY =================

    @PostMapping("/category")
    public ResponseEntity<CategoryResponseDto> addCategory(@Valid @RequestBody CategoryRequestDto dto) {
        return ResponseEntity.ok(menuService.addCategory(dto));
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<CategoryResponseDto> getCategory(@PathVariable Long id) throws ResourceNotFoundException {
        return ResponseEntity.ok(menuService.getCategoryById(id));
    }

    @GetMapping("/categories/restaurant/{restaurantId}")
    public ResponseEntity<List<CategoryResponseDto>> getCategoriesByRestaurant(@PathVariable Long restaurantId) throws RestaurantNotFoundException {
        return ResponseEntity.ok(menuService.getCategoriesByRestaurant(restaurantId));
    }

    @PutMapping("/category/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
                        @PathVariable Long id, @Valid @RequestBody CategoryRequestDto dto) 
                        throws ResourceNotFoundException{
        return ResponseEntity.ok(menuService.updateCategory(id, dto));
    }

    @DeleteMapping("/category/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) 
                    throws ResourceNotFoundException{
        menuService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully");
    }

    // ================= MENU ITEM =================

    @PostMapping("/item")
    public ResponseEntity<MenuItemResponseDto> addMenuItem(@Valid @RequestBody MenuItemRequestDto dto) 
                    throws ResourceNotFoundException{
        return ResponseEntity.ok(menuService.addMenuItem(dto));
    }

    @GetMapping("/item/{id}")
    public ResponseEntity<MenuItemResponseDto> getMenuItem(@PathVariable Long id)
                        throws ResourceNotFoundException {
        return ResponseEntity.ok(menuService.getMenuItemById(id));
    }

    @GetMapping("/items/restaurant/{restaurantId}")
    public ResponseEntity<List<MenuItemResponseDto>> getItemsByRestaurant(@PathVariable Long restaurantId)
                            throws RestaurantNotFoundException {
        return ResponseEntity.ok(menuService.getItemsByRestaurant(restaurantId));
    }

    @GetMapping("/items/category/{categoryId}")
    public ResponseEntity<List<MenuItemResponseDto>> getItemsByCategory(@PathVariable Long categoryId) 
                        throws ResourceNotFoundException{
        return ResponseEntity.ok(menuService.getMenuItemsByCategory(categoryId));
    }

    @PutMapping("/item/{id}")
    public ResponseEntity<MenuItemResponseDto> updateMenuItem(
            @PathVariable Long id, @Valid @RequestBody MenuItemRequestDto dto) 
            throws ResourceNotFoundException {
        return ResponseEntity.ok(menuService.updateMenuItem(id, dto));
    }

    @PatchMapping("/item/{id}/availability")
    public ResponseEntity<MenuItemResponseDto> toggleAvailability(
            @PathVariable Long id, @RequestParam boolean available) 
            throws ResourceNotFoundException{
        return ResponseEntity.ok(menuService.toggleAvailability(id, available));
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<String> deleteMenuItem(@PathVariable Long id)
                    throws ResourceNotFoundException{
        menuService.deleteMenuItem(id);
        return ResponseEntity.ok("Item deleted successfully");
    }

    // ================= MENU VIEW =================

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<CategoryResponseDto>> getFullMenu(@PathVariable Long restaurantId) 
                            throws RestaurantNotFoundException{
        return ResponseEntity.ok(menuService.getFullMenuByRestaurant(restaurantId));
    }

    // ================= FILTERS =================

    @GetMapping("/search")
    public ResponseEntity<List<MenuItemResponseDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(menuService.searchMenuItems(query));
    }

    @GetMapping("/veg")
    public ResponseEntity<List<MenuItemResponseDto>> getVegItems() {
        return ResponseEntity.ok(menuService.getItemsByVeg(true));
    }

    @GetMapping("/non-veg")
    public ResponseEntity<List<MenuItemResponseDto>> getNonVegItems() {
        return ResponseEntity.ok(menuService.getItemsByVeg(false));
    }

    @GetMapping("/price")
    public ResponseEntity<List<MenuItemResponseDto>> getItemsBelowPrice(@RequestParam BigDecimal price) {
        return ResponseEntity.ok(menuService.getItemsBelowPrice(price));
    }
}