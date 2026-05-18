package com.quickbite.auth.authservice.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpDispatchResponseDto {

    private String verificationId;

    private String maskedEmail;

    private long expiresInSeconds;

    private String message;
}
