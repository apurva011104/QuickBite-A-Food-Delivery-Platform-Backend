package com.quickbite.delivery.deliveryservice.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CompleteDeliveryRequestDto {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Delivery OTP is required")
    @Pattern(regexp = "\\d{6}", message = "Delivery OTP must be a 6-digit code")
    private String otp;
}
