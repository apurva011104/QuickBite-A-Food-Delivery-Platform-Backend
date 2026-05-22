package com.quickbite.review.reviewservice.dto.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Delivery rating is required")
    @Min(value = 1, message = "Delivery rating must be at least 1")
    @Max(value = 5, message = "Delivery rating must be at most 5")
    private Integer deliveryRating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    @NotEmpty(message = "At least one menu item review is required")
    @Valid
    private List<MenuItemReviewRequestDto> itemReviews;
}
