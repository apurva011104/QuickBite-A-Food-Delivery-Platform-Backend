package com.quickbite.delivery.deliveryservice.dto.requestDto;

import java.math.BigDecimal;

import com.quickbite.delivery.deliveryservice.entity.VehicleType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliveryAgentRequestDto {

    @NotNull
    private String userId;

    @NotNull
    private String fullName;

    @NotNull
    private String phone;

    @NotNull
    private VehicleType vehicleType;

    @NotNull
    private String vehicleNumber;

    @NotNull
    private BigDecimal currentLatitude;

    @NotNull
    private BigDecimal currentLongitude;
}