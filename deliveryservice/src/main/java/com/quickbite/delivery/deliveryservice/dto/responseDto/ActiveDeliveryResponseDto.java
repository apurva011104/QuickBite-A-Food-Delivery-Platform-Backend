package com.quickbite.delivery.deliveryservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveDeliveryResponseDto {

    private Long orderId;
    private Long agentId;
    private String status;
}