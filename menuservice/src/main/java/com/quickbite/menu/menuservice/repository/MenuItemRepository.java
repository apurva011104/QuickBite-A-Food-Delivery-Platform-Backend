package com.quickbite.menu.menuservice.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.menu.menuservice.entity.MenuCategory;
import com.quickbite.menu.menuservice.entity.MenuItem;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long>{

    Optional<MenuItem> findByItemId(Long itemId);

    List<MenuItem> findByRestaurantId(Long restaurantId);

    List<MenuItem> findByCategory(MenuCategory category);

    List<MenuItem> findByIsVeg(boolean veg);

    List<MenuItem> findByNameContainingIgnoreCase(String name);

    List<MenuItem> findByPriceLessThanEqual(BigDecimal price);

    List<MenuItem> findByIsAvailable(boolean available);

    List<MenuItem> findByRatingLessThanEqual(double rating);

    List<MenuItem> findByRatingGreaterThanEqual(double rating);
    
    long countByRestaurantId(Long restaurantId);

}
