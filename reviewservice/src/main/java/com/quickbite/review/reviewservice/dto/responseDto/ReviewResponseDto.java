package com.quickbite.review.reviewservice.dto.responseDto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ReviewResponseDto {

    private Long reviewId;
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private Long agentId;
    private Integer foodRating;
    private Integer deliveryRating;
    private String comment;
    private Boolean verified;
    private LocalDateTime reviewDate;
    private LocalDateTime updatedAt;
    private List<MenuItemReviewResponseDto> itemReviews;
}
