package com.quickbite.review.reviewservice.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewUpdateRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.MessageResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.entity.Review;
import com.quickbite.review.reviewservice.exception.BadRequestException;
import com.quickbite.review.reviewservice.exception.ResourceNotFoundException;
import com.quickbite.review.reviewservice.mapper.ReviewMapper;
import com.quickbite.review.reviewservice.repository.ReviewRepository;
import com.quickbite.review.reviewservice.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public ReviewResponseDto addReview(ReviewRequestDto requestDto) {
        log.info("Adding review for orderId: {}", requestDto.getOrderId());

        if (reviewRepository.existsByOrderId(requestDto.getOrderId())) {
            log.warn("Review already exists for orderId: {}", requestDto.getOrderId());
            throw new BadRequestException("Review already exists for this order");
        }

        Review review = reviewMapper.toEntity(requestDto);
        Review savedReview = reviewRepository.save(review);

        log.info("Review created successfully with reviewId: {}", savedReview.getReviewId());
        return reviewMapper.toResponseDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getByReviewId(Long reviewId) {
        log.info("Fetching review by reviewId: {}", reviewId);

        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        return reviewMapper.toResponseDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getByOrderId(Long orderId) {
        log.info("Fetching review by orderId: {}", orderId);

        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for order ID: " + orderId));

        return reviewMapper.toResponseDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByRestaurantId(Long restaurantId) {
        log.info("Fetching reviews by restaurantId: {}", restaurantId);

        return reviewRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByCustomerId(Long customerId) {
        log.info("Fetching reviews by customerId: {}", customerId);

        return reviewRepository.findByCustomerId(customerId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByAgentId(Long agentId) {
        log.info("Fetching reviews by agentId: {}", agentId);

        return reviewRepository.findByAgentId(agentId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Override
    public ReviewResponseDto updateReview(Long reviewId, ReviewUpdateRequestDto requestDto) {
        log.info("Updating reviewId: {}", reviewId);

        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (requestDto.getFoodRating() != null) {
            review.setFoodRating(requestDto.getFoodRating());
        }

        if (requestDto.getDeliveryRating() != null) {
            review.setDeliveryRating(requestDto.getDeliveryRating());
        }

        if (requestDto.getComment() != null) {
            review.setComment(requestDto.getComment());
        }

        if (requestDto.getVerified() != null) {
            review.setVerified(requestDto.getVerified());
        }

        Review updatedReview = reviewRepository.save(review);

        log.info("Review updated successfully for reviewId: {}", reviewId);
        return reviewMapper.toResponseDto(updatedReview);
    }

    @Override
    public MessageResponseDto deleteReview(Long reviewId) {
        log.info("Deleting reviewId: {}", reviewId);

        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        reviewRepository.delete(review);

        log.info("Review deleted successfully for reviewId: {}", reviewId);
        return new MessageResponseDto("Review deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgFoodRating(Long restaurantId) {
        log.info("Calculating average food rating for restaurantId: {}", restaurantId);

        List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(Review::getFoodRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgDeliveryRating(Long agentId) {
        log.info("Calculating average delivery rating for agentId: {}", agentId);

        List<Review> reviews = reviewRepository.findByAgentId(agentId);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(Review::getDeliveryRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        log.info("Fetching all reviews");

        return reviewRepository.findAll()
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }
}