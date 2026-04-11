package com.quickbite.menu.menuservice.entity;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name = "menu_items")
public class MenuItem {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long restaurantId;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    @JsonBackReference
    private MenuCategory category;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal discountedPrice;

    private String imageUrl;

    @Column(nullable = false)
    private boolean isAvailable = true;

    @Column(nullable = false)
    private boolean isVeg;

    @Column(nullable = false)
    private double rating = 0.0;

    @Column(nullable = false)
    private double calories;

    @ElementCollection
    private List<String> tags;

    public MenuItem(long restaurantId, MenuCategory category, String name, 
                String description, BigDecimal price, BigDecimal discountedPrice, 
                String imageUrl, boolean isVeg, double calories, List<String> tags) {

        this.restaurantId = restaurantId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountedPrice = discountedPrice;
        this.imageUrl = imageUrl;
        this.isVeg = isVeg;
        this.calories = calories;
        this.tags = tags;

    }
}
