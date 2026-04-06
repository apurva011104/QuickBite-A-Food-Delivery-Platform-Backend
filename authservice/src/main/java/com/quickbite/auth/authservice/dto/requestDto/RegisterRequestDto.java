package com.quickbite.auth.authservice.dto.requestDto;

import com.quickbite.auth.authservice.entity.Role;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RegisterRequestDto {

    @NotNull(message="Name cannot be null or empty")
    private String name;

    @NotNull(message="Email cannot be null or empty")
    private String email;

    @Nullable
    private String phoneNumber;

    @Nullable
    private String password;

    @NotNull(message="Role cannot be null or empty")
    private Role role;

}
