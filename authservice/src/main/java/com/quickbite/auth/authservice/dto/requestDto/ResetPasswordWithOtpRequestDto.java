package com.quickbite.auth.authservice.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordWithOtpRequestDto {

    @NotBlank
    private String verificationId;

    @NotBlank
    private String otp;

    @NotBlank
    private String newPassword;
}
