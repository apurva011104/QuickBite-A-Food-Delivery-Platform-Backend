package com.quickbite.review.reviewservice.dto.responseDto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MenuItemReviewResponseDto {

    private Long menuItemReviewId;
    private Long orderId;
    private Long menuItemId;
    private String itemName;
    private Integer rating;
    private String comment;
    private Boolean verified;
    private LocalDateTime reviewDate;
    private LocalDateTime updatedAt;
}
