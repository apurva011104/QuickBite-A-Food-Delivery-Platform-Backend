package com.quickbite.delivery.deliveryservice.dto.requestDto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationUpdateRequestDto {

    @NotNull(message = "Latitude is required")
    private BigDecimal currentLatitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal currentLongitude;
}