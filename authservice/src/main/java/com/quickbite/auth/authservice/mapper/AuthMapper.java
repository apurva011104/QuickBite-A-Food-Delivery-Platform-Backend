package com.quickbite.auth.authservice.mapper;

import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.dto.responseDto.UserProfileResponseDto;
import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.User;

public class AuthMapper {

    private AuthMapper() {
    }

    public static User registerDtoToUser(RegisterRequestDto dto, AuthProvider provider) {
        return new User(
                dto.getName(),
                dto.getEmail(),
                dto.getPhoneNumber(),
                dto.getPassword(),
                dto.getRole(),
                provider
        );
    }

    public static AuthResponseDto userToAuthResponse(User user, String accessToken) {
        return new AuthResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                accessToken
        );
    }

    public static UserProfileResponseDto userToProfileResponse(User user) {
        return new UserProfileResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getAuthProvider(),
                user.isActive()
        );
    }
}
