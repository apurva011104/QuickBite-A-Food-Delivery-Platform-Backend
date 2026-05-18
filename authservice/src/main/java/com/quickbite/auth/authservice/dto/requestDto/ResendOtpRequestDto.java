package com.quickbite.auth.authservice.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequestDto {

    @NotBlank(message = "Verification ID is required")
    private String verificationId;
}
