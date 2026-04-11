package com.quickbite.menu.menuservice.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="menu_categories")
public class MenuCategory {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long categoryId;
    
    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable=false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<MenuItem> items = new ArrayList<>();

    public MenuCategory(Long restaurantId, String name, 
                        String description, String imageUrl,
                        int displayOrder) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.items = new ArrayList<>();
    }
}
