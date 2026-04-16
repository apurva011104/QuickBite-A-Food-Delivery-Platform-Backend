package com.quickbite.delivery.deliveryservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAgentResponseDto {
    
    private Long agentId;
    private String fullName;
    private String phone;
    private String vehicleType;
    private boolean isAvailable;
    private boolean isVerified;
    private double avgRating;

}
