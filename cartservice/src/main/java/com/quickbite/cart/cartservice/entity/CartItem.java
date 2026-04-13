package com.quickbite.cart.cartservice.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long cartId;

    @Column(nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = true)
    private String customization;

    public CartItem(Long cartId, Long menuItemId, String name, Integer quantity, BigDecimal price) {
        this.cartId = cartId;
        this.menuItemId = menuItemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public CartItem(Long cartId, Long menuItemId, String name, Integer quantity, BigDecimal price, String customization) {
        this.cartId = cartId;
        this.menuItemId = menuItemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.customization = customization;
    }
}
