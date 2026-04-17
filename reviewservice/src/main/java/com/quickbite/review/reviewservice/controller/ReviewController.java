package com.quickbite.review.reviewservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewUpdateRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.MessageResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponseDto> addReview(@Valid @RequestBody ReviewRequestDto requestDto) {
        log.info("API HIT - Add review for orderId: {}", requestDto.getOrderId());
        return new ResponseEntity<>(reviewService.addReview(requestDto), HttpStatus.CREATED);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getByReviewId(@PathVariable Long reviewId) {
        log.info("API HIT - Get review by reviewId: {}", reviewId);
        return ResponseEntity.ok(reviewService.getByReviewId(reviewId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ReviewResponseDto> getByOrderId(@PathVariable Long orderId) {
        log.info("API HIT - Get review by orderId: {}", orderId);
        return ResponseEntity.ok(reviewService.getByOrderId(orderId));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReviewResponseDto>> getByRestaurantId(@PathVariable Long restaurantId) {
        log.info("API HIT - Get reviews by restaurantId: {}", restaurantId);
        return ResponseEntity.ok(reviewService.getByRestaurantId(restaurantId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ReviewResponseDto>> getByCustomerId(@PathVariable Long customerId) {
        log.info("API HIT - Get reviews by customerId: {}", customerId);
        return ResponseEntity.ok(reviewService.getByCustomerId(customerId));
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<ReviewResponseDto>> getByAgentId(@PathVariable Long agentId) {
        log.info("API HIT - Get reviews by agentId: {}", agentId);
        return ResponseEntity.ok(reviewService.getByAgentId(agentId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequestDto requestDto) {
        log.info("API HIT - Update reviewId: {}", reviewId);
        return ResponseEntity.ok(reviewService.updateReview(reviewId, requestDto));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<MessageResponseDto> deleteReview(@PathVariable Long reviewId) {
        log.info("API HIT - Delete reviewId: {}", reviewId);
        return ResponseEntity.ok(reviewService.deleteReview(reviewId));
    }

    @GetMapping("/avg-food/{restaurantId}")
    public ResponseEntity<Double> getAvgFoodRating(@PathVariable Long restaurantId) {
        log.info("API HIT - Get avg food rating for restaurantId: {}", restaurantId);
        return ResponseEntity.ok(reviewService.getAvgFoodRating(restaurantId));
    }

    @GetMapping("/avg-delivery/{agentId}")
    public ResponseEntity<Double> getAvgDeliveryRating(@PathVariable Long agentId) {
        log.info("API HIT - Get avg delivery rating for agentId: {}", agentId);
        return ResponseEntity.ok(reviewService.getAvgDeliveryRating(agentId));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponseDto>> getAllReviews() {
        log.info("API HIT - Get all reviews");
        return ResponseEntity.ok(reviewService.getAllReviews());
    }
}