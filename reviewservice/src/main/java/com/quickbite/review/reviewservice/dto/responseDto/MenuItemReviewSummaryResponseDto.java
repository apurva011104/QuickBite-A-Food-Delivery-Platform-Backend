package com.quickbite.review.reviewservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuItemReviewSummaryResponseDto {

    private Long menuItemId;
    private Double averageRating;
    private Long reviewCount;
}
