package com.quickbite.review.reviewservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.review.reviewservice.entity.MenuItemReview;

@Repository
public interface MenuItemReviewRepository extends JpaRepository<MenuItemReview, Long> {

    List<MenuItemReview> findByOrderId(Long orderId);

    List<MenuItemReview> findByMenuItemId(Long menuItemId);

    long countByMenuItemId(Long menuItemId);
}
