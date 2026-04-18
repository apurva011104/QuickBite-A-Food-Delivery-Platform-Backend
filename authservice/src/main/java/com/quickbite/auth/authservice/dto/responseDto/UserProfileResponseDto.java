package com.quickbite.auth.authservice.dto.responseDto;

import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponseDto {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Role role;
    private AuthProvider authProvider;
    private boolean active;
}