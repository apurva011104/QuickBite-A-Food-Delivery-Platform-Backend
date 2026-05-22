package com.quickbite.delivery.deliveryservice.client.dto;

import lombok.Data;

@Data
public class OrderSummaryResponseDto {

    private Long orderId;
    private Long restaurantId;
    private Long customerId;
    private Long deliveryAgentId;
    private String orderStatus;
}
