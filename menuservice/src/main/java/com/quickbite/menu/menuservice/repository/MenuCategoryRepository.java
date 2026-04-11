package com.quickbite.menu.menuservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.menu.menuservice.entity.MenuCategory;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long>{
    
    List<MenuCategory> findByCategoryId(Long categoryId);

    List<MenuCategory> findByName(String name);

}
