package com.quickbite.review.reviewservice.dto.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewUpdateRequestDto {

    @Min(value = 1, message = "Food rating must be at least 1")
    @Max(value = 5, message = "Food rating must be at most 5")
    private Integer foodRating;

    @Min(value = 1, message = "Delivery rating must be at least 1")
    @Max(value = 5, message = "Delivery rating must be at most 5")
    private Integer deliveryRating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}