package com.quickbite.review.reviewservice.mapper;

import java.util.List;

import com.quickbite.review.reviewservice.dto.responseDto.MenuItemReviewResponseDto;
import org.springframework.stereotype.Component;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.entity.MenuItemReview;
import com.quickbite.review.reviewservice.entity.Review;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Review review = new Review();
        review.setOrderId(dto.getOrderId());
        review.setDeliveryRating(dto.getDeliveryRating());
        review.setComment(dto.getComment());

        return review;
    }

    public ReviewResponseDto toResponseDto(Review review) {
        if (review == null) {
            return null;
        }

        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setReviewId(review.getReviewId());
        dto.setOrderId(review.getOrderId());
        dto.setCustomerId(review.getCustomerId());
        dto.setRestaurantId(review.getRestaurantId());
        dto.setAgentId(review.getAgentId());
        dto.setFoodRating(review.getFoodRating());
        dto.setDeliveryRating(review.getDeliveryRating());
        dto.setComment(review.getComment());
        dto.setVerified(review.getVerified());
        dto.setReviewDate(review.getReviewDate());
        dto.setUpdatedAt(review.getUpdatedAt());

        return dto;
    }

    public List<MenuItemReviewResponseDto> toMenuItemResponseDtos(List<MenuItemReview> itemReviews) {
        return itemReviews.stream().map(this::toMenuItemResponseDto).toList();
    }

    public MenuItemReviewResponseDto toMenuItemResponseDto(MenuItemReview itemReview) {
        if (itemReview == null) {
            return null;
        }

        MenuItemReviewResponseDto dto = new MenuItemReviewResponseDto();
        dto.setMenuItemReviewId(itemReview.getMenuItemReviewId());
        dto.setOrderId(itemReview.getOrderId());
        dto.setMenuItemId(itemReview.getMenuItemId());
        dto.setItemName(itemReview.getItemName());
        dto.setRating(itemReview.getRating());
        dto.setComment(itemReview.getComment());
        dto.setVerified(itemReview.getVerified());
        dto.setReviewDate(itemReview.getReviewDate());
        dto.setUpdatedAt(itemReview.getUpdatedAt());
        return dto;
    }
}
