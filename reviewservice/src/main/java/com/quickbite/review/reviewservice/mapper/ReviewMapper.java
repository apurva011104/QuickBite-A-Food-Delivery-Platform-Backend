package com.quickbite.review.reviewservice.mapper;

import org.springframework.stereotype.Component;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.entity.Review;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Review review = new Review();
        review.setOrderId(dto.getOrderId());
        review.setRestaurantId(dto.getRestaurantId());
        review.setAgentId(dto.getAgentId());
        review.setFoodRating(dto.getFoodRating());
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
}