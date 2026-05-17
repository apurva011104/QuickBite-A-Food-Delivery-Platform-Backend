package com.quickbite.auth.authservice.dto.responseDto;

import com.quickbite.auth.authservice.entity.Role;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {

    @NotNull
    private Long id;
    
    @NotNull
    private String name;

    @NotNull
    private String email;

    private String phoneNumber;
    
    @NotNull
    private Role role;
    
    @NotNull
    private String accessToken;
}
