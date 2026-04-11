package com.quickbite.menu.menuservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.menu.menuservice.entity.MenuCategory;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long>{
    
    Optional<MenuCategory> findByCategoryId(Long categoryId);

    List<MenuCategory> findByNameContainingIgnoreCase(String name);

    List<MenuCategory> findByRestaurantId(Long restaurantId);

}
