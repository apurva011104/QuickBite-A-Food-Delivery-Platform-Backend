package com.quickbite.review.reviewservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.review.reviewservice.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReviewId(Long reviewId);

    Optional<Review> findByOrderId(Long orderId);

    List<Review> findByRestaurantId(Long restaurantId);

    List<Review> findByCustomerId(Long customerId);

    List<Review> findByAgentId(Long agentId);

    boolean existsByOrderId(Long orderId);

    long countByRestaurantId(Long restaurantId);
}