package com.quickbite.review.reviewservice.external.order.dto;

import java.util.List;

import lombok.Data;

@Data
public class OrderSummaryResponseDto {

    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private Long deliveryAgentId;
    private String orderStatus;
    private List<OrderItemSummaryResponseDto> items;
}
