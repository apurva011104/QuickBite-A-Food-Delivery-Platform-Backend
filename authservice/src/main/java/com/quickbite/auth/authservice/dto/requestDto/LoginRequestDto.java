package com.quickbite.auth.authservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginRequestDto {
    
    @NotNull(message="Login identifier cannot be null or empty")
    private String identifier;

    @NotNull(message="Password cannot be null or empty")
    private String password;

    @NotNull(message="Login type cannot be null")
    private LoginType loginType;
}
