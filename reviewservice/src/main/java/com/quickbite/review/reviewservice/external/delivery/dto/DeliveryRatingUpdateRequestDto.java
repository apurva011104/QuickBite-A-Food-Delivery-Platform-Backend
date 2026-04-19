package com.quickbite.review.reviewservice.external.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryRatingUpdateRequestDto {
    private Integer newRating;
}