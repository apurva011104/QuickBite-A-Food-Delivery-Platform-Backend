package com.quickbite.review.reviewservice.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewModerationRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewUpdateRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.MessageResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.entity.Review;
import com.quickbite.review.reviewservice.exception.BadRequestException;
import com.quickbite.review.reviewservice.exception.ResourceNotFoundException;
import com.quickbite.review.reviewservice.exception.UnauthorizedActionException;
import com.quickbite.review.reviewservice.external.delivery.client.DeliveryClient;
import com.quickbite.review.reviewservice.external.delivery.dto.DeliveryRatingUpdateRequestDto;
import com.quickbite.review.reviewservice.external.restaurant.client.RestaurantClient;
import com.quickbite.review.reviewservice.mapper.ReviewMapper;
import com.quickbite.review.reviewservice.repository.ReviewRepository;
import com.quickbite.review.reviewservice.security.UserPrincipal;
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
    private final RestaurantClient restaurantClient;
    private final DeliveryClient deliveryClient;

    @Override
    public ReviewResponseDto addReview(UserPrincipal currentUser, ReviewRequestDto requestDto) {
        log.info("Adding review for orderId={} customerId={}", requestDto.getOrderId(), currentUser.getUserId());

        if (reviewRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new BadRequestException("Review already exists for this order");
        }

        Review review = reviewMapper.toEntity(requestDto);
        review.setCustomerId(currentUser.getUserId());
        review.setVerified(false);

        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully reviewId={}", savedReview.getReviewId());

        pushRatings(savedReview);

        return reviewMapper.toResponseDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getByReviewId(Long reviewId) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        return reviewMapper.toResponseDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getByOrderId(Long orderId) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for order ID: " + orderId));

        return reviewMapper.toResponseDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByCustomerId(Long customerId, UserPrincipal currentUser) {
        if (!customerId.equals(currentUser.getUserId()) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedActionException("You are not allowed to view these reviews");
        }

        return reviewRepository.findByCustomerId(customerId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByAgentId(Long agentId) {
        return reviewRepository.findByAgentId(agentId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Override
    public ReviewResponseDto updateReview(Long reviewId, UserPrincipal currentUser, ReviewUpdateRequestDto requestDto) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to update this review");
        }

        if (requestDto.getFoodRating() != null) {
            review.setFoodRating(requestDto.getFoodRating());
        }

        if (requestDto.getDeliveryRating() != null) {
            review.setDeliveryRating(requestDto.getDeliveryRating());
        }

        if (requestDto.getComment() != null) {
            review.setComment(requestDto.getComment());
        }

        review.setVerified(false);

        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully reviewId={}", reviewId);

        pushRatings(updatedReview);

        return reviewMapper.toResponseDto(updatedReview);
    }

    @Override
    public ReviewResponseDto moderateReview(Long reviewId, ReviewModerationRequestDto requestDto) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        review.setVerified(requestDto.getVerified());
        Review updatedReview = reviewRepository.save(review);

        log.info("Review moderation updated reviewId={} verified={}", reviewId, requestDto.getVerified());
        return reviewMapper.toResponseDto(updatedReview);
    }

    @Override
    public MessageResponseDto deleteReview(Long reviewId, UserPrincipal currentUser) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getCustomerId().equals(currentUser.getUserId()) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedActionException("You are not allowed to delete this review");
        }

        reviewRepository.delete(review);
        log.info("Review deleted successfully reviewId={}", reviewId);

        return new MessageResponseDto("Review deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgFoodRating(Long restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);
        if (reviews.isEmpty()) return 0.0;

        return reviews.stream()
                .mapToInt(Review::getFoodRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgDeliveryRating(Long agentId) {
        List<Review> reviews = reviewRepository.findByAgentId(agentId);
        if (reviews.isEmpty()) return 0.0;

        return reviews.stream()
                .mapToInt(Review::getDeliveryRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    private void pushRatings(Review review) {
        try {
            restaurantClient.updateRestaurantRating(
                    review.getRestaurantId(),
                    review.getFoodRating().doubleValue()
            );
            log.info("Restaurant rating pushed restaurantId={} rating={}",
                    review.getRestaurantId(), review.getFoodRating());
        } catch (Exception ex) {
            log.error("Failed to push restaurant rating for restaurantId={}", review.getRestaurantId(), ex);
        }

        try {
            deliveryClient.updateDeliveryRating(
                    review.getAgentId(),
                    new DeliveryRatingUpdateRequestDto(review.getDeliveryRating())
            );
            log.info("Delivery rating pushed agentId={} rating={}",
                    review.getAgentId(), review.getDeliveryRating());
        } catch (Exception ex) {
            log.error("Failed to push delivery rating for agentId={}", review.getAgentId(), ex);
        }
    }
}