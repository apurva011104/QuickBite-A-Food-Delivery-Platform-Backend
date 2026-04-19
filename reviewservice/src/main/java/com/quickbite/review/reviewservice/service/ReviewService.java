package com.quickbite.review.reviewservice.service;

import java.util.List;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewModerationRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewUpdateRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.MessageResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.security.UserPrincipal;

public interface ReviewService {

    ReviewResponseDto addReview(UserPrincipal currentUser, ReviewRequestDto requestDto);

    ReviewResponseDto getByReviewId(Long reviewId);

    ReviewResponseDto getByOrderId(Long orderId);

    List<ReviewResponseDto> getByRestaurantId(Long restaurantId);

    List<ReviewResponseDto> getByCustomerId(Long customerId, UserPrincipal currentUser);

    List<ReviewResponseDto> getByAgentId(Long agentId);

    ReviewResponseDto updateReview(Long reviewId, UserPrincipal currentUser, ReviewUpdateRequestDto requestDto);

    ReviewResponseDto moderateReview(Long reviewId, ReviewModerationRequestDto requestDto);

    MessageResponseDto deleteReview(Long reviewId, UserPrincipal currentUser);

    Double getAvgFoodRating(Long restaurantId);

    Double getAvgDeliveryRating(Long agentId);

    List<ReviewResponseDto> getAllReviews();
}