package com.quickbite.cart.cartservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.cart.cartservice.entity.CartItem;


public interface CartItemRepository extends JpaRepository<CartItem, Long>{

    List<CartItem> findByCartId(Long cartId);

    void deleteByCartId(Long cartId);

    List<CartItem> findByMenuItemId(Long menuItemId);

    List<CartItem> findByNameContainingIgnoreCase(String name);
}
