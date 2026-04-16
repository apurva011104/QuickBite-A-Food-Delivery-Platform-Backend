package com.quickbite.delivery.deliveryservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignResponseDto {
    private Long agentId;
    private String fullName;
    private String phone;
}