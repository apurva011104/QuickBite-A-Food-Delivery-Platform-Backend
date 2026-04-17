package com.quickbite.delivery.deliveryservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerificationRequestDto {

    @NotNull(message = "Verification status is required")
    private Boolean verified;
}