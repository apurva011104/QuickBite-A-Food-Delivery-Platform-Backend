package com.quickbite.delivery.deliveryservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityUpdateRequestDto {

    @NotNull(message = "Availability status is required")
    private Boolean available;
}