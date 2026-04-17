package com.quickbite.review.reviewservice.service;

import java.util.List;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewUpdateRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.MessageResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;

public interface ReviewService {

    ReviewResponseDto addReview(ReviewRequestDto requestDto);

    ReviewResponseDto getByReviewId(Long reviewId);

    ReviewResponseDto getByOrderId(Long orderId);

    List<ReviewResponseDto> getByRestaurantId(Long restaurantId);

    List<ReviewResponseDto> getByCustomerId(Long customerId);

    List<ReviewResponseDto> getByAgentId(Long agentId);

    ReviewResponseDto updateReview(Long reviewId, ReviewUpdateRequestDto requestDto);

    MessageResponseDto deleteReview(Long reviewId);

    Double getAvgFoodRating(Long restaurantId);

    Double getAvgDeliveryRating(Long agentId);

    List<ReviewResponseDto> getAllReviews();
}