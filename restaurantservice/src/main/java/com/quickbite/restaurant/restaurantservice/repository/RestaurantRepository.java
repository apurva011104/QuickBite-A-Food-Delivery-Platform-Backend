package com.quickbite.restaurant.restaurantservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.restaurant.restaurantservice.entity.Restaurant;


public interface RestaurantRepository extends JpaRepository<Restaurant, Long>{

    List<Restaurant> findByOwnerId(Long ownerId);

    List<Restaurant> findByIsApprovedTrue();

    List<Restaurant> findByCityAndIsApprovedTrue(String city);

    Optional<Restaurant> findByRestaurantIdAndIsApprovedTrue(Long id);

    List<Restaurant> findByIsApprovedFalse();

    Optional<Restaurant> findByRestaurantIdAndOwnerId(Long restaurantId, Long ownerId);

    List<Restaurant> findByCuisineIgnoreCaseAndIsApprovedTrue(String cuisine);

    List<Restaurant> findByNameContainingIgnoreCaseAndIsApprovedTrue(String keyword);
}
