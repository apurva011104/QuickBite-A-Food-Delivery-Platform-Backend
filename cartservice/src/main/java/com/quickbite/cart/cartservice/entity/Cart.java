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
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long cartId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public Cart(Long customerId, Long restaurantId) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalPrice = BigDecimal.ZERO;
    }
    
    public Cart(Long customerId, Long restaurantId, BigDecimal totalPrice) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalPrice = totalPrice;
    }
}
