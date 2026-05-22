package com.quickbite.delivery.deliveryservice.dto.responseDto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryCompletionOtpResponseDto {

    private Long orderId;
    private String otp;
    private String status;
    private LocalDateTime generatedAt;
}
