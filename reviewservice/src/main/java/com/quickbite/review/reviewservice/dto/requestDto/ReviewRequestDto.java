package com.quickbite.review.reviewservice.dto.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Food rating is required")
    @Min(value = 1, message = "Food rating must be at least 1")
    @Max(value = 5, message = "Food rating must be at most 5")
    private Integer foodRating;

    @NotNull(message = "Delivery rating is required")
    @Min(value = 1, message = "Delivery rating must be at least 1")
    @Max(value = 5, message = "Delivery rating must be at most 5")
    private Integer deliveryRating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}