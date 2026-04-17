package com.quickbite.delivery.deliveryservice.dto.responseDto;

import java.math.BigDecimal;

import com.quickbite.delivery.deliveryservice.enums.VehicleType;

import lombok.Data;

@Data
public class DeliveryAgentResponseDto {

    private Long agentId;
    private Long userId;
    private String fullName;
    private String phone;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private boolean available;
    private boolean verified;
    private BigDecimal avgRating;
    private Integer totalDeliveries;
}