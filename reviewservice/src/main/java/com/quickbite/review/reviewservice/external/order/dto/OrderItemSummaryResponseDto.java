package com.quickbite.review.reviewservice.external.order.dto;

import lombok.Data;

@Data
public class OrderItemSummaryResponseDto {

    private Long orderItemId;
    private Long menuItemId;
    private String name;
}
