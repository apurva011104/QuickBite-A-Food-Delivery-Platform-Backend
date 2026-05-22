package com.quickbite.review.reviewservice.dto.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuItemReviewRequestDto {

    @NotNull(message = "Menu item ID is required")
    private Long menuItemId;

    @NotNull(message = "Menu item rating is required")
    @Min(value = 1, message = "Menu item rating must be at least 1")
    @Max(value = 5, message = "Menu item rating must be at most 5")
    private Integer rating;

    @Size(max = 500, message = "Menu item comment must not exceed 500 characters")
    private String comment;
}
