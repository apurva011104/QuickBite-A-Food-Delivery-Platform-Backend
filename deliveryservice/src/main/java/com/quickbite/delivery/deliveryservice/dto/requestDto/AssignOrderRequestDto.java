package com.quickbite.delivery.deliveryservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignOrderRequestDto {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Order ID is required")
    private Long orderId;
}