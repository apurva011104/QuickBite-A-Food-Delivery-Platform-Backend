package com.quickbite.cart.cartservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.cart.cartservice.entity.Cart;


public interface CartRepository extends JpaRepository<Cart, Long>{
    Optional<Cart> findByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);

    List<Cart> findByRestaurantId(Long restaurantId);

    void deleteByCustomerId(Long customerId);
}
